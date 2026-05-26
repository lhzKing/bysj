package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.core.util.IdUtil;
import com.example.trace.common.BizException;
import com.example.trace.config.TraceDemoDataProperties;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.SysUser;
import com.example.trace.entity.TraceAggregation;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceFlowTaskMapper;
import com.example.trace.mapper.TraceFlowTaskScanMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.mapper.TraceScanIdempotencyMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.TraceDemoDataService;
import com.example.trace.service.impl.support.DemoAggregationFactory;
import com.example.trace.service.impl.support.DemoChainBuilder;
import com.example.trace.service.impl.support.DemoFlowTaskFactory;
import com.example.trace.service.impl.support.DemoUserRef;
import com.example.trace.service.impl.support.TraceBatchCommitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * One-stop demo data generator covering 8 business tables in a single call:
 * trace_assign_batch / trace_code / trace_lifecycle_log / trace_snapshot /
 * trace_flow_task / trace_flow_task_scan / trace_aggregation.
 *
 * <p>Master data (trace_node / base_part_spec / sys_user demo accounts /
 * trace_user_node_binding) must already exist — call
 * {@code POST /api/admin/seed-master-data} first, or run
 * {@code scripts/seed_extended_data.py} as an offline fallback.</p>
 *
 * <p>Persistence follows the T-P1-01 two-phase contract: stage 1 builds every
 * entity (including RSA signatures) in memory with no transaction; stage 2
 * commits per-table in chunks via {@link TraceBatchCommitter}, each chunk in
 * its own {@code REQUIRES_NEW} transaction.</p>
 */
@Service
public class TraceDemoDataServiceImpl implements TraceDemoDataService {

    private static final Logger log = LoggerFactory.getLogger(TraceDemoDataServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Tunable: how many trace codes per batch. The Python reference seeder uses (15, 25).
    private static final int TRACE_CODES_PER_BATCH_MIN = 15;
    private static final int TRACE_CODES_PER_BATCH_MAX = 25;

    private final BasePartSpecMapper partSpecMapper;
    private final TraceLifecycleLogMapper logMapper;
    private final TraceSnapshotMapper snapshotMapper;
    private final TraceCodeMapper traceCodeMapper;
    private final TraceAssignBatchMapper assignBatchMapper;
    private final TraceFlowTaskMapper flowTaskMapper;
    private final TraceFlowTaskScanMapper flowTaskScanMapper;
    private final TraceAggregationMapper aggregationMapper;
    private final TraceScanIdempotencyMapper scanIdempotencyMapper;
    private final TraceNodeMapper traceNodeMapper;
    private final SysUserMapper sysUserMapper;
    private final TraceDemoDataProperties traceDemoDataProperties;
    private final TraceBatchCommitter batchCommitter;
    private final DemoChainBuilder chainBuilder;
    private final DemoFlowTaskFactory flowTaskFactory;
    private final DemoAggregationFactory aggregationFactory;
    private final Random random = new Random();

    public TraceDemoDataServiceImpl(
            BasePartSpecMapper partSpecMapper,
            TraceLifecycleLogMapper logMapper,
            TraceSnapshotMapper snapshotMapper,
            TraceCodeMapper traceCodeMapper,
            TraceAssignBatchMapper assignBatchMapper,
            TraceFlowTaskMapper flowTaskMapper,
            TraceFlowTaskScanMapper flowTaskScanMapper,
            TraceAggregationMapper aggregationMapper,
            TraceScanIdempotencyMapper scanIdempotencyMapper,
            TraceNodeMapper traceNodeMapper,
            SysUserMapper sysUserMapper,
            TraceDemoDataProperties traceDemoDataProperties,
            TraceBatchCommitter batchCommitter,
            DemoChainBuilder chainBuilder,
            DemoFlowTaskFactory flowTaskFactory,
            DemoAggregationFactory aggregationFactory
    ) {
        this.partSpecMapper = partSpecMapper;
        this.logMapper = logMapper;
        this.snapshotMapper = snapshotMapper;
        this.traceCodeMapper = traceCodeMapper;
        this.assignBatchMapper = assignBatchMapper;
        this.flowTaskMapper = flowTaskMapper;
        this.flowTaskScanMapper = flowTaskScanMapper;
        this.aggregationMapper = aggregationMapper;
        this.scanIdempotencyMapper = scanIdempotencyMapper;
        this.traceNodeMapper = traceNodeMapper;
        this.sysUserMapper = sysUserMapper;
        this.traceDemoDataProperties = traceDemoDataProperties;
        this.batchCommitter = batchCommitter;
        this.chainBuilder = chainBuilder;
        this.flowTaskFactory = flowTaskFactory;
        this.aggregationFactory = aggregationFactory;
    }

    @Override
    public Map<String, Object> generateSampleData(int count, String operator, String operatorRole) {
        ensureAdminOperationEnabled("generate-sample-data", operator, operatorRole);
        validateGenerateCount(count, operator, operatorRole);
        log.info("Trace demo data generation started: operator={}, role={}, count={}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole), count);
        long started = System.currentTimeMillis();

        // ----- Pre-flight: master data must exist -----
        List<TraceNode> allNodes = traceNodeMapper.selectList(
                new QueryWrapper<TraceNode>().eq("enabled", 1));
        List<BasePartSpec> allParts = partSpecMapper.selectList(
                new QueryWrapper<BasePartSpec>().eq("enabled", 1));
        ensureMasterDataReady(allNodes, allParts);
        List<TraceNode> factories  = allNodes.stream().filter(n -> "FACTORY".equals(n.getNodeType())).toList();
        List<TraceNode> warehouses = allNodes.stream().filter(n -> "WAREHOUSE".equals(n.getNodeType())).toList();
        List<TraceNode> logistics  = allNodes.stream().filter(n -> "LOGISTICS".equals(n.getNodeType())).toList();
        List<TraceNode> customers  = allNodes.stream().filter(n -> "CUSTOMER".equals(n.getNodeType())).toList();
        if (factories.isEmpty() || warehouses.isEmpty() || logistics.isEmpty() || customers.isEmpty()) {
            throw BizException.badRequest(
                    "demo 主数据不完整：每种节点类型（FACTORY/WAREHOUSE/LOGISTICS/CUSTOMER）都需至少 1 个，请先调用 POST /api/admin/seed-master-data");
        }

        // Resolve demo users by role for operator + creator references.
        List<DemoUserRef> producerUsers = lookupDemoUsersByRole("PRODUCER");
        List<DemoUserRef> warehouseUsers = lookupDemoUsersByRole("WAREHOUSE");
        List<DemoUserRef> logisticsUsers = lookupDemoUsersByRole("LOGISTICS");
        if (producerUsers.isEmpty() || warehouseUsers.isEmpty() || logisticsUsers.isEmpty()) {
            throw BizException.badRequest(
                    "demo 主数据不完整：需要至少各 1 个 PRODUCER / WAREHOUSE / LOGISTICS demo 用户，请先调用 POST /api/admin/seed-master-data");
        }

        // ----- Stage 1: build everything in memory (NO transaction, NO connection held) -----
        LocalDateTime baseTime = LocalDateTime.of(2026, 4, 1, 9, 13, 0);
        BuildOutput out = buildAllInMemory(count, allParts, factories, warehouses, logistics, customers,
                producerUsers, warehouseUsers, logisticsUsers, baseTime);

        // ----- Stage 2: chunked REQUIRES_NEW commits, per-table -----
        batchCommitter.commitAssignBatchesInChunks(out.batches);
        // Back-fill trace_code.batch_id from batch business key now that auto-id is populated.
        Map<String, Long> batchNoToId = new HashMap<>();
        for (TraceAssignBatch b : out.batches) {
            batchNoToId.put(b.getBatchNo(), b.getId());
        }
        for (TraceBatchCommitter.DemoTraceWithCodeUnit unit : out.traceUnits) {
            TraceCode code = unit.traceCode();
            if (code.getBatchId() == null) {
                Long resolved = batchNoToId.get(((TraceCodeWithBatchNo) code).pendingBatchNo);
                code.setBatchId(resolved);
            }
        }
        batchCommitter.commitDemoUnitsWithCodeInChunks(out.traceUnits);
        batchCommitter.commitFlowTasksInChunks(out.flowTaskUnits);
        batchCommitter.commitAggregationsInChunks(out.aggregations);

        long durationMillis = System.currentTimeMillis() - started;
        int lifecycleLogs = out.traceUnits.stream().mapToInt(u -> u.logs().size()).sum();
        int flowTaskScans = out.flowTaskUnits.stream().mapToInt(u -> u.scans().size()).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batches", out.batches.size());
        result.put("traceCodes", out.traceUnits.size());
        result.put("lifecycleLogs", lifecycleLogs);
        result.put("snapshots", out.traceUnits.size());
        result.put("flowTasks", out.flowTaskUnits.size());
        result.put("flowTaskScans", flowTaskScans);
        result.put("aggregations", out.aggregations.size());
        result.put("durationMillis", durationMillis);

        log.info("Trace demo data generation completed: operator={}, role={}, result={}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole), result);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> clearTraceData(String operator, String operatorRole) {
        ensureAdminOperationEnabled("clear-trace-data", operator, operatorRole);
        log.warn("Trace demo data clear started: operator={}, role={}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole));

        // FK delete order — children before parents:
        //   trace_flow_task_scan      → fk_trace_flow_task_scan_task (CASCADE) on trace_flow_task
        //   trace_flow_task           → no inbound FK among business tables
        //   trace_aggregation         → no inbound FK on trace_code (parent/child are plain VARCHAR)
        //   trace_scan_idempotency    → fk_trace_scan_idempotency_log on trace_lifecycle_log
        //   trace_lifecycle_log       → self-ref fk_correction_of (delete handles self-ref OK)
        //   trace_snapshot            → no inbound FK
        //   trace_code                → fk_trace_code_batch (SET NULL) on trace_assign_batch
        //   trace_assign_batch        → fk_trace_assign_batch_spu RESTRICT on base_part_spec
        //                               (we DON'T touch base_part_spec, so RESTRICT never trips)
        long flowTaskScans = flowTaskScanMapper.selectCount(null);
        flowTaskScanMapper.delete(null);
        long flowTasks = flowTaskMapper.selectCount(null);
        flowTaskMapper.delete(null);
        long aggregations = aggregationMapper.selectCount(null);
        aggregationMapper.delete(null);
        long idempotencyKeys = scanIdempotencyMapper.selectCount(null);
        scanIdempotencyMapper.delete(null);
        long logs = logMapper.selectCount(null);
        logMapper.delete(null);
        long snapshots = snapshotMapper.selectCount(null);
        snapshotMapper.delete(null);
        long traceCodes = traceCodeMapper.selectCount(null);
        traceCodeMapper.delete(null);
        long batches = assignBatchMapper.selectCount(null);
        assignBatchMapper.delete(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deletedFlowTaskScans", flowTaskScans);
        result.put("deletedFlowTasks", flowTasks);
        result.put("deletedAggregations", aggregations);
        result.put("deletedIdempotencyKeys", idempotencyKeys);
        result.put("deletedLogs", logs);
        result.put("deletedSnapshots", snapshots);
        result.put("deletedTraceCodes", traceCodes);
        result.put("deletedBatches", batches);

        log.warn("Trace demo data clear completed: operator={}, role={}, result={}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole), result);
        return result;
    }

    // -----------------------------------------------------------------------
    // Stage 1 — in-memory build
    // -----------------------------------------------------------------------

    private BuildOutput buildAllInMemory(int count,
                                         List<BasePartSpec> parts,
                                         List<TraceNode> factories,
                                         List<TraceNode> warehouses,
                                         List<TraceNode> logistics,
                                         List<TraceNode> customers,
                                         List<DemoUserRef> producers,
                                         List<DemoUserRef> warehouseUsers,
                                         List<DemoUserRef> logisticsUsers,
                                         LocalDateTime baseTime) {
        List<TraceAssignBatch> batches = new ArrayList<>();
        List<TraceBatchCommitter.DemoTraceWithCodeUnit> traceUnits = new ArrayList<>(count);
        // Each in-flight trace's terminal status — used downstream to pick task scan / aggregation candidates.
        List<DemoFlowTaskFactory.ScanCandidate> aggregatableCodes = new ArrayList<>();

        int remaining = count;
        int batchSeq = 0;
        String runId = IdUtil.fastSimpleUUID().substring(0, 6);
        while (remaining > 0) {
            int bucketSize = Math.min(remaining,
                    TRACE_CODES_PER_BATCH_MIN + random.nextInt(TRACE_CODES_PER_BATCH_MAX - TRACE_CODES_PER_BATCH_MIN + 1));
            batchSeq++;
            BasePartSpec spu = parts.get(random.nextInt(parts.size()));
            DemoUserRef producer = producers.get(batchSeq % producers.size());
            TraceNode factory = factories.get(batchSeq % factories.size());
            LocalDateTime batchTime = baseTime.plusDays(batchSeq - 1L).plusHours((batchSeq - 1L) % 6)
                    .truncatedTo(ChronoUnit.SECONDS);

            TraceAssignBatch batch = new TraceAssignBatch();
            batch.setBatchNo("ASSIGN-EXT-%04d-%s-%s".formatted(batchSeq, spu.getPartCode(), runId));
            batch.setProductionOrderNo("PO-%s-%04d".formatted(batchTime.format(DATE_FMT), batchSeq));
            batch.setSpuId(spu.getId());
            batch.setQuantityRequested(bucketSize);
            batch.setQuantityGenerated(bucketSize);
            batch.setQuantityPrinted(bucketSize);
            batch.setQuantityActivated(bucketSize);
            batch.setManufacturerNodeId(factory.getId());
            batch.setStatus("GENERATED");
            batch.setOperatorId(producer.id());
            batch.setOperatorUsername(producer.username());
            batches.add(batch);

            for (int i = 0; i < bucketSize; i++) {
                String traceCode = "TC-EXT-%04d-%04d-%s".formatted(batchSeq, i + 1, shortUuid());
                LocalDateTime initTime = batchTime.plusMinutes(5L + i);
                TraceNode warehouse = warehouses.get(random.nextInt(warehouses.size()));
                TraceNode logisticsNode = logistics.get(random.nextInt(logistics.size()));
                TraceNode customer = customers.get(random.nextInt(customers.size()));
                DemoUserRef warehouseOp = warehouseUsers.get(random.nextInt(warehouseUsers.size()));
                DemoUserRef logisticsOp = logisticsUsers.get(random.nextInt(logisticsUsers.size()));

                DemoChainBuilder.ChainResult chain = chainBuilder.buildChain(
                        traceCode, spu.getId(),
                        factory, warehouse, logisticsNode, customer,
                        producer.username(), warehouseOp.username(), logisticsOp.username(),
                        initTime, random);

                // Find the ACTIVATE_CODE timestamp for trace_code.activated_time
                LocalDateTime activatedTime = chain.logs().stream()
                        .filter(l -> "ACTIVATE_CODE".equals(l.getActionType()))
                        .map(TraceLifecycleLog::getEventTime)
                        .findFirst().orElse(initTime);

                TraceCodeWithBatchNo traceCodeRow = new TraceCodeWithBatchNo();
                traceCodeRow.setTraceCode(traceCode);
                traceCodeRow.pendingBatchNo = batch.getBatchNo(); // back-filled to batchId in stage 2
                traceCodeRow.setSpuId(spu.getId());
                traceCodeRow.setSerialNo(i + 1);
                traceCodeRow.setQrPayload("{\"traceCode\":\"" + traceCode + "\",\"spuId\":" + spu.getId() + ",\"v\":1}");
                traceCodeRow.setCodeStatus(mapSnapshotStatusToCodeStatus(chain.terminalStatus()));
                traceCodeRow.setPrintCount(1);
                traceCodeRow.setActivatedTime(activatedTime);
                traceCodeRow.setActivatedBy(producer.id());
                traceCodeRow.setActivatedByUsername(producer.username());
                traceCodeRow.setCurrentSnapshotId(traceCode);

                traceUnits.add(new TraceBatchCommitter.DemoTraceWithCodeUnit(
                        chain.logs(), chain.snapshot(), traceCodeRow));

                // Eligible for tasks/aggregations: trace currently sitting in inventory or in flight.
                if ("IN_STOCK".equals(chain.terminalStatus()) || "IN_TRANSIT".equals(chain.terminalStatus())) {
                    aggregatableCodes.add(new DemoFlowTaskFactory.ScanCandidate(traceCode));
                }
            }
            remaining -= bucketSize;
        }

        // Flow tasks (60 of them, drawn from the aggregatable pool)
        List<TraceBatchCommitter.FlowTaskWithScansUnit> flowTaskUnits = flowTaskFactory.build(
                        aggregatableCodes, warehouses, logistics, customers,
                        warehouseUsers, logisticsUsers,
                        baseTime, random)
                .stream()
                .map(u -> new TraceBatchCommitter.FlowTaskWithScansUnit(u.task(), u.scans()))
                .toList();

        // Aggregations (24 cartons + 6 pallets)
        List<String> aggregationCandidates = aggregatableCodes.stream()
                .map(DemoFlowTaskFactory.ScanCandidate::traceCode)
                .collect(Collectors.toList());
        DemoUserRef aggregationCreator = producers.isEmpty() ? null : producers.get(0);
        List<TraceAggregation> aggregations = aggregationFactory.build(
                aggregationCandidates, aggregationCreator, baseTime, random);

        return new BuildOutput(batches, traceUnits, flowTaskUnits, aggregations);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void ensureMasterDataReady(List<TraceNode> nodes, List<BasePartSpec> parts) {
        if (nodes.isEmpty() || parts.isEmpty()) {
            throw BizException.badRequest(
                    "demo 主数据未就绪：trace_node / base_part_spec 为空，请先调用 POST /api/admin/seed-master-data");
        }
    }

    private List<DemoUserRef> lookupDemoUsersByRole(String roleCode) {
        // Resolve role_id first, then fetch sys_user rows with that role.
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.inSql("role_id",
                "SELECT id FROM sys_role WHERE role_code = '" + roleCode.replace("'", "''") + "'");
        wrapper.eq("status", 1);
        return sysUserMapper.selectList(wrapper).stream()
                .map(u -> new DemoUserRef(u.getId(), u.getUsername()))
                .toList();
    }

    private static String mapSnapshotStatusToCodeStatus(String snapshotStatus) {
        // trace_code.code_status taxonomy: GENERATED/PRINTED/ACTIVATED/IN_STOCK/IN_TRANSIT/EXCEPTION/VOIDED/SCRAPPED
        return switch (snapshotStatus) {
            case "INIT" -> "ACTIVATED"; // a trace that completed the chain (INIT→PRINT→ACTIVATE) is at minimum activated
            case "IN_STOCK" -> "IN_STOCK";
            case "IN_TRANSIT" -> "IN_TRANSIT";
            case "TRANSFERRED" -> "IN_STOCK"; // delivered to customer, treated as in-stock on receiver side
            case "EXCEPTION" -> "EXCEPTION";
            default -> "ACTIVATED";
        };
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private void ensureAdminOperationEnabled(String operation, String operator, String operatorRole) {
        if (traceDemoDataProperties.isEnabled()) {
            return;
        }
        log.warn("Trace demo admin operation rejected because endpoint is disabled: operation={}, operator={}, role={}",
                operation, normalizeAuditValue(operator), normalizeAuditValue(operatorRole));
        throw BizException.forbidden("当前环境已禁用示例数据管理接口");
    }

    private void validateGenerateCount(int count, String operator, String operatorRole) {
        int maxGenerateCount = traceDemoDataProperties.getMaxGenerateCount();
        if (count >= TraceDemoDataProperties.MIN_GENERATE_COUNT && count <= maxGenerateCount) {
            return;
        }
        log.warn("Trace demo data generation rejected due to invalid count: operator={}, role={}, count={}, allowedRange={}..{}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole), count,
                TraceDemoDataProperties.MIN_GENERATE_COUNT, maxGenerateCount);
        throw BizException.badRequest("count 必须在 " + TraceDemoDataProperties.MIN_GENERATE_COUNT
                + " 到 " + maxGenerateCount + " 之间");
    }

    private static String normalizeAuditValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    // -----------------------------------------------------------------------
    // Private types
    // -----------------------------------------------------------------------

    /**
     * Carries the not-yet-resolved batch business key from stage 1 to stage 2.
     * Once stage 2 commits the batches and back-fills {@link #pendingBatchNo}
     * → {@link com.example.trace.entity.TraceAssignBatch#getId()} via the map,
     * {@link com.example.trace.entity.TraceCode#getBatchId()} is set.
     */
    private static class TraceCodeWithBatchNo extends TraceCode {
        String pendingBatchNo;
    }

    private record BuildOutput(
            List<TraceAssignBatch> batches,
            List<TraceBatchCommitter.DemoTraceWithCodeUnit> traceUnits,
            List<TraceBatchCommitter.FlowTaskWithScansUnit> flowTaskUnits,
            List<TraceAggregation> aggregations
    ) {}
}
