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

    private static final int SAMPLE_LIFECYCLE_PATH_LIMIT = 3;
    private static final List<String> DEMO_CORE_ACTION_PREFIX = List.of(
            "INIT", "PRINT_CODE", "ACTIVATE_CODE", "INBOUND"
    );
    private static final List<String> DEMO_ACTION_COUNT_ORDER = List.of(
            "INIT", "PRINT_CODE", "ACTIVATE_CODE", "INBOUND",
            "OUTBOUND", "TRANSFER", "DELIVER",
            "PACK", "PALLETIZE", "EXCEPTION_OPEN"
    );

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
        result.put("lifecycleValidation", out.lifecycleStats.validationStatus());
        result.put("lifecycleValidationErrors", out.lifecycleStats.validationErrors());
        result.put("lifecycleModel", lifecycleModelDescription());
        result.put("coreLifecyclePrefix", DEMO_CORE_ACTION_PREFIX);
        result.put("actionCounts", out.lifecycleStats.orderedActionCounts());
        result.put("snapshotStatusCounts", out.lifecycleStats.orderedSnapshotStatusCounts());
        result.put("codeStatusCounts", out.lifecycleStats.orderedCodeStatusCounts());
        result.put("terminalSummary", out.lifecycleStats.terminalSummary());
        result.put("sampleLifecyclePaths", out.lifecycleStats.sampleLifecyclePaths());

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
        //   trace_lifecycle_log       → self-ref fk_correction_of; clear correction_of first, then delete
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
        int clearedCorrectionReferences = logMapper.clearCorrectionReferences();
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
        result.put("clearedCorrectionReferences", clearedCorrectionReferences);
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
        // Stage 1a: build chains first WITHOUT freezing them into units, so the
        // aggregation factory below can still mutate each chain (append PACK /
        // PALLETIZE logs + bump snapshot.lastHash) before we hand them to the
        // committer. Once a DemoTraceWithCodeUnit is constructed its logs are
        // copied with List.copyOf and become immutable.
        List<PendingTraceUnit> pendingUnits = new ArrayList<>(count);
        Map<String, DemoChainBuilder.ChainResult> chainsByTrace = new LinkedHashMap<>();
        // Each in-flight trace's terminal status — used downstream to pick task scan candidates.
        List<DemoFlowTaskFactory.ScanCandidate> flowTaskScanCandidates = new ArrayList<>();
        // Aggregation (PACK/PALLETIZE) must happen while goods are still in stock;
        // do not append packing logs to IN_TRANSIT or TRANSFERRED demo chains.
        List<String> aggregationCandidateCodes = new ArrayList<>();
        DemoLifecycleStats lifecycleStats = new DemoLifecycleStats();

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

                validateGeneratedLifecycle(traceCodeRow, chain);
                lifecycleStats.acceptTrace(traceCodeRow, chain);

                pendingUnits.add(new PendingTraceUnit(chain, traceCodeRow));
                chainsByTrace.put(traceCode, chain);

                // Flow-task scans can demonstrate either stock-out or receive-in-transit.
                if ("IN_STOCK".equals(chain.terminalStatus()) || "IN_TRANSIT".equals(chain.terminalStatus())) {
                    flowTaskScanCandidates.add(new DemoFlowTaskFactory.ScanCandidate(traceCode));
                }
                // PACK/PALLETIZE must occur before outbound/while still in stock, matching the real aggregation service.
                if ("IN_STOCK".equals(chain.terminalStatus())) {
                    aggregationCandidateCodes.add(traceCode);
                }
            }
            remaining -= bucketSize;
        }

        // Flow tasks (60 of them, drawn from the task-scan candidate pool).
        // Very small demo counts may by chance leave no code in IN_STOCK/IN_TRANSIT;
        // in that case the factory still creates task headers but no scan rows.
        List<TraceBatchCommitter.FlowTaskWithScansUnit> flowTaskUnits = flowTaskFactory.build(
                        flowTaskScanCandidates, warehouses, logistics, customers,
                        warehouseUsers, logisticsUsers,
                        baseTime, random)
                .stream()
                .map(u -> new TraceBatchCommitter.FlowTaskWithScansUnit(u.task(), u.scans()))
                .toList();

        // Aggregations (24 cartons + 6 pallets). The factory mutates each
        // chain in place: appends PACK / PALLETIZE logs and updates the tail
        // hash + event time on the snapshot. We must run this BEFORE freezing
        // chains into DemoTraceWithCodeUnit (List.copyOf inside the record).
        DemoUserRef aggregationCreator = producers.isEmpty() ? null : producers.get(0);
        List<TraceAggregation> aggregations = aggregationFactory.build(
                aggregationCandidateCodes, chainsByTrace, aggregationCreator, baseTime, random);
        lifecycleStats.refreshPostAggregation(pendingUnits);

        // Stage 1b: now that PACK/PALLETIZE logs are appended, freeze chains
        // into immutable DemoTraceWithCodeUnit records ready for commit.
        List<TraceBatchCommitter.DemoTraceWithCodeUnit> traceUnits = new ArrayList<>(pendingUnits.size());
        for (PendingTraceUnit pending : pendingUnits) {
            traceUnits.add(new TraceBatchCommitter.DemoTraceWithCodeUnit(
                    pending.chain.logs(), pending.chain.snapshot(), pending.traceCode));
        }

        return new BuildOutput(batches, traceUnits, flowTaskUnits, aggregations, lifecycleStats);
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
        // trace_code.code_status taxonomy: GENERATED/PRINTED/ACTIVATED/IN_STOCK/IN_TRANSIT/TRANSFERRED/EXCEPTION/VOIDED/SCRAPPED
        return switch (snapshotStatus) {
            case "INIT" -> "ACTIVATED"; // a trace that completed the chain (INIT→PRINT→ACTIVATE) is at minimum activated
            case "IN_STOCK" -> "IN_STOCK";
            case "IN_TRANSIT" -> "IN_TRANSIT";
            case "TRANSFERRED" -> "TRANSFERRED"; // delivered to customer; terminal, cannot be received again
            case "EXCEPTION" -> "EXCEPTION";
            default -> "ACTIVATED";
        };
    }

    private static void validateGeneratedLifecycle(TraceCode traceCodeRow, DemoChainBuilder.ChainResult chain) {
        List<String> actions = chain.logs().stream()
                .map(TraceLifecycleLog::getActionType)
                .toList();
        String traceCode = traceCodeRow.getTraceCode();

        requireCondition(actions.size() >= DEMO_CORE_ACTION_PREFIX.size(),
                traceCode + ": 生命周期日志少于核心链路长度");
        requireCondition(actions.subList(0, DEMO_CORE_ACTION_PREFIX.size()).equals(DEMO_CORE_ACTION_PREFIX),
                traceCode + ": 示例链路必须从 INIT -> PRINT_CODE -> ACTIVATE_CODE -> INBOUND 开始，actual=" + actions);

        int inboundIndex = actions.indexOf("INBOUND");
        int outboundIndex = actions.indexOf("OUTBOUND");
        int transferIndex = actions.indexOf("TRANSFER");
        int deliverIndex = actions.indexOf("DELIVER");
        int exceptionIndex = actions.indexOf("EXCEPTION_OPEN");

        requireCondition(inboundIndex == 3,
                traceCode + ": ACTIVATE_CODE 后必须先成品入库，不能直接出库");
        requireCondition(outboundIndex < 0 || outboundIndex > inboundIndex,
                traceCode + ": OUTBOUND 必须发生在首次 INBOUND 之后");
        requireCondition(transferIndex < 0 || (outboundIndex >= 0 && transferIndex > outboundIndex),
                traceCode + ": TRANSFER 只能发生在 OUTBOUND 进入运输中之后");
        requireCondition(deliverIndex < 0 || (outboundIndex >= 0 && deliverIndex > outboundIndex),
                traceCode + ": DELIVER 只能发生在 OUTBOUND 进入运输中之后");
        requireCondition(transferIndex < 0 || deliverIndex < 0 || transferIndex < deliverIndex,
                traceCode + ": TRANSFER 不能发生在最终交付 DELIVER 之后");
        requireCondition(exceptionIndex < 0 || exceptionIndex == actions.size() - 1,
                traceCode + ": EXCEPTION_OPEN 只能作为示例链尾冻结状态");

        if ("TRANSFERRED".equals(chain.terminalStatus())) {
            requireCondition(deliverIndex >= 0,
                    traceCode + ": snapshot=TRANSFERRED 必须由 DELIVER 产生");
            requireCondition(deliverIndex == actions.size() - 1,
                    traceCode + ": DELIVER 进入 TRANSFERRED 后必须是终态，不能再追加入库/异常");
            requireCondition("TRANSFERRED".equals(traceCodeRow.getCodeStatus()),
                    traceCode + ": 已交付快照必须同步 trace_code.code_status=TRANSFERRED");
        }
        if ("TRANSFERRED".equals(traceCodeRow.getCodeStatus())) {
            requireCondition(deliverIndex >= 0,
                    traceCode + ": trace_code.code_status=TRANSFERRED 必须存在 DELIVER 日志");
        }
        if ("IN_TRANSIT".equals(chain.terminalStatus())) {
            requireCondition(outboundIndex >= 0,
                    traceCode + ": IN_TRANSIT 必须由 OUTBOUND 产生");
            requireCondition(deliverIndex < 0,
                    traceCode + ": IN_TRANSIT 链路不能已存在 DELIVER");
        }
        if ("IN_STOCK".equals(chain.terminalStatus())) {
            requireCondition(deliverIndex < 0,
                    traceCode + ": IN_STOCK 链路不能已存在 DELIVER");
        }
        if ("EXCEPTION".equals(chain.terminalStatus())) {
            requireCondition(exceptionIndex == actions.size() - 1,
                    traceCode + ": EXCEPTION 快照必须由链尾 EXCEPTION_OPEN 产生");
            requireCondition(chain.snapshot().getExceptionRestoreStatus() != null,
                    traceCode + ": EXCEPTION 快照必须记录冻结前可恢复状态");
            requireCondition(!"EXCEPTION".equals(chain.snapshot().getExceptionRestoreStatus()),
                    traceCode + ": EXCEPTION restoreStatus 不能仍为 EXCEPTION");
        }
    }

    private static void requireCondition(boolean condition, String message) {
        if (!condition) {
            throw BizException.serverError("generate-sample-data 生命周期校验失败：" + message);
        }
    }

    private static String lifecycleModelDescription() {
        return "码状态 GENERATED -> PRINTED -> ACTIVATED；商品状态 INIT -> INBOUND -> IN_STOCK -> OUTBOUND -> IN_TRANSIT -> TRANSFER*(仍为 IN_TRANSIT) -> INBOUND 循环 或 DELIVER -> TRANSFERRED(终态)";
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

    /**
     * Intermediate stage-1 carrier holding a chain and its pending trace_code
     * row before the chain logs are frozen into a
     * {@link TraceBatchCommitter.DemoTraceWithCodeUnit}. Lets {@code
     * DemoAggregationFactory} append PACK / PALLETIZE entries to the same
     * underlying ArrayList that the committer will see.
     */
    private record PendingTraceUnit(
            DemoChainBuilder.ChainResult chain,
            TraceCodeWithBatchNo traceCode
    ) {}

    /**
     * Lifecycle statistics surfaced by generate-sample-data so callers can see
     * that the API generated data against the intended state model, not just
     * opaque row counts.
     */
    private static class DemoLifecycleStats {

        private final Map<String, Integer> actionCounts = new LinkedHashMap<>();
        private final Map<String, Integer> snapshotStatusCounts = new LinkedHashMap<>();
        private final Map<String, Integer> codeStatusCounts = new LinkedHashMap<>();
        private final List<String> sampleLifecyclePaths = new ArrayList<>(SAMPLE_LIFECYCLE_PATH_LIMIT);
        private final List<String> validationErrors = new ArrayList<>();
        private int totalChains;
        private int finishedGoodsInboundBeforeOutboundChains;
        private int inStockChains;
        private int inTransitChains;
        private int deliveredTerminalChains;
        private int exceptionChains;

        void acceptTrace(TraceCode traceCode, DemoChainBuilder.ChainResult chain) {
            totalChains++;
            chain.logs().forEach(logEntry -> increment(actionCounts, logEntry.getActionType()));
            increment(snapshotStatusCounts, chain.snapshot().getCurrentStatus());
            increment(codeStatusCounts, traceCode.getCodeStatus());
            if (sampleLifecyclePaths.size() < SAMPLE_LIFECYCLE_PATH_LIMIT) {
                sampleLifecyclePaths.add(chain.logs().stream()
                        .map(TraceLifecycleLog::getActionType)
                        .collect(Collectors.joining(" -> ")));
            }

            switch (chain.snapshot().getCurrentStatus()) {
                case "IN_STOCK" -> inStockChains++;
                case "IN_TRANSIT" -> inTransitChains++;
                case "TRANSFERRED" -> deliveredTerminalChains++;
                case "EXCEPTION" -> exceptionChains++;
                default -> {
                    // INIT should not be terminal in generated demo data because
                    // every code is printed, activated and stocked-in. Preserve
                    // the count in snapshotStatusCounts; validation catches it.
                }
            }

            List<String> actions = chain.logs().stream()
                    .map(TraceLifecycleLog::getActionType)
                    .toList();
            int firstInbound = actions.indexOf("INBOUND");
            int firstOutbound = actions.indexOf("OUTBOUND");
            if (firstInbound >= 0 && (firstOutbound < 0 || firstInbound < firstOutbound)) {
                finishedGoodsInboundBeforeOutboundChains++;
            }
            collectValidationErrors(traceCode, chain, actions);
        }

        /**
         * PACK/PALLETIZE are appended after base trace rows are created. Refresh
         * only the mutable log-derived fields; terminal status and code status
         * are intentionally unchanged by aggregation actions.
         */
        void refreshPostAggregation(List<PendingTraceUnit> pendingUnits) {
            actionCounts.clear();
            sampleLifecyclePaths.clear();
            for (PendingTraceUnit pending : pendingUnits) {
                pending.chain().logs().forEach(logEntry -> increment(actionCounts, logEntry.getActionType()));
                if (sampleLifecyclePaths.size() < SAMPLE_LIFECYCLE_PATH_LIMIT) {
                    sampleLifecyclePaths.add(pending.chain().logs().stream()
                            .map(TraceLifecycleLog::getActionType)
                            .collect(Collectors.joining(" -> ")));
                }
            }
        }

        Map<String, Integer> orderedActionCounts() {
            return orderedCounts(actionCounts, DEMO_ACTION_COUNT_ORDER);
        }

        Map<String, Integer> orderedSnapshotStatusCounts() {
            return orderedCounts(snapshotStatusCounts, List.of(
                    "INIT", "IN_STOCK", "IN_TRANSIT", "TRANSFERRED", "EXCEPTION"
            ));
        }

        Map<String, Integer> orderedCodeStatusCounts() {
            return orderedCounts(codeStatusCounts, List.of(
                    "GENERATED", "PRINTED", "ACTIVATED", "IN_STOCK",
                    "IN_TRANSIT", "TRANSFERRED", "EXCEPTION", "VOIDED", "SCRAPPED"
            ));
        }

        Map<String, Object> terminalSummary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalChains", totalChains);
            summary.put("finishedGoodsInboundBeforeOutboundChains", finishedGoodsInboundBeforeOutboundChains);
            summary.put("inStockChains", inStockChains);
            summary.put("inTransitChains", inTransitChains);
            summary.put("deliveredTerminalChains", deliveredTerminalChains);
            summary.put("exceptionChains", exceptionChains);
            summary.put("transferredTerminalBlockedFromFurtherInbound", true);
            summary.put("transferMeansTransitOnly", true);
            summary.put("deliverMeansFinalTransferred", true);
            return summary;
        }

        String validationStatus() {
            return validationErrors.isEmpty() ? "OK" : "FAILED";
        }

        List<String> validationErrors() {
            return List.copyOf(validationErrors);
        }

        List<String> sampleLifecyclePaths() {
            return List.copyOf(sampleLifecyclePaths);
        }

        private void collectValidationErrors(
                TraceCode traceCode,
                DemoChainBuilder.ChainResult chain,
                List<String> actions
        ) {
            String traceCodeValue = traceCode.getTraceCode();
            addIf(actions.size() < DEMO_CORE_ACTION_PREFIX.size()
                            || !actions.subList(0, DEMO_CORE_ACTION_PREFIX.size()).equals(DEMO_CORE_ACTION_PREFIX),
                    traceCodeValue + ": missing core lifecycle prefix");

            int firstInbound = actions.indexOf("INBOUND");
            int firstOutbound = actions.indexOf("OUTBOUND");
            int transfer = actions.indexOf("TRANSFER");
            int deliver = actions.indexOf("DELIVER");
            int exceptionOpen = actions.indexOf("EXCEPTION_OPEN");

            addIf(firstInbound != 3, traceCodeValue + ": first INBOUND must immediately follow ACTIVATE_CODE");
            addIf(firstOutbound >= 0 && firstOutbound < firstInbound,
                    traceCodeValue + ": OUTBOUND before finished-goods INBOUND");
            addIf(transfer >= 0 && (firstOutbound < 0 || transfer < firstOutbound),
                    traceCodeValue + ": TRANSFER before OUTBOUND");
            addIf(deliver >= 0 && (firstOutbound < 0 || deliver < firstOutbound),
                    traceCodeValue + ": DELIVER before OUTBOUND");
            addIf(transfer >= 0 && deliver >= 0 && transfer > deliver,
                    traceCodeValue + ": TRANSFER after DELIVER");
            addIf(exceptionOpen >= 0 && exceptionOpen != actions.size() - 1,
                    traceCodeValue + ": EXCEPTION_OPEN is not tail action");
            addIf("TRANSFERRED".equals(chain.snapshot().getCurrentStatus()) && deliver < 0,
                    traceCodeValue + ": TRANSFERRED snapshot without DELIVER");
            addIf("TRANSFERRED".equals(chain.snapshot().getCurrentStatus()) && deliver >= 0 && deliver != actions.size() - 1,
                    traceCodeValue + ": DELIVER/TRANSFERRED is not terminal tail action");
            addIf("TRANSFERRED".equals(traceCode.getCodeStatus()) && deliver < 0,
                    traceCodeValue + ": TRANSFERRED code status without DELIVER");
        }

        private void addIf(boolean invalid, String message) {
            if (invalid) {
                validationErrors.add(message);
            }
        }

        private static void increment(Map<String, Integer> target, String key) {
            if (key == null || key.isBlank()) {
                key = "UNKNOWN";
            }
            target.merge(key, 1, Integer::sum);
        }

        private static Map<String, Integer> orderedCounts(Map<String, Integer> source, List<String> preferredOrder) {
            Map<String, Integer> ordered = new LinkedHashMap<>();
            for (String key : preferredOrder) {
                if (source.containsKey(key)) {
                    ordered.put(key, source.get(key));
                }
            }
            source.entrySet().stream()
                    .filter(entry -> !ordered.containsKey(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
            return ordered;
        }
    }

    private record BuildOutput(
            List<TraceAssignBatch> batches,
            List<TraceBatchCommitter.DemoTraceWithCodeUnit> traceUnits,
            List<TraceBatchCommitter.FlowTaskWithScansUnit> flowTaskUnits,
            List<TraceAggregation> aggregations,
            DemoLifecycleStats lifecycleStats
    ) {}
}
