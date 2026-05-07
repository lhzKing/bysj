package com.example.trace.service.impl;

import cn.hutool.core.util.IdUtil;
import com.example.trace.common.BizException;
import com.example.trace.config.TraceDemoDataProperties;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.TraceDemoDataService;
import com.example.trace.service.impl.support.TraceBatchCommitter;
import com.example.trace.service.impl.support.TraceBatchCommitter.DemoTraceUnit;
import com.example.trace.util.HashUtil;
import com.example.trace.util.ProvinceUtil;
import com.example.trace.util.SignatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Demo data generator with two-phase persistence (T-P1-01).
 *
 * <p>Stage 1 (no transaction): build all lifecycle logs + snapshots in memory,
 * including RSA signing. Stage 2: chunked {@code REQUIRES_NEW} commits via
 * {@link TraceBatchCommitter}. Part-spec creation stays in its own
 * {@code @Transactional} helper because it's a small fixed-size operation
 * (≤ 5 rows) and lives ahead of the heavy loop.</p>
 */
@Service
public class TraceDemoDataServiceImpl implements TraceDemoDataService {

    private static final Logger log = LoggerFactory.getLogger(TraceDemoDataServiceImpl.class);
    private static final String GENESIS_HASH = "GENESIS";
    private static final DateTimeFormatter TRACE_CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final List<PartDef> PART_DEFINITIONS = List.of(
            new PartDef("SPU-VALVE-002", "气动球阀", "阀门类", "V-2024002", "上海阀门厂"),
            new PartDef("SPU-BEAR-001", "深沟球轴承", "轴承类", "B-6205", "SKF中国"),
            new PartDef("SPU-MOTOR-001", "三相异步电机", "电机类", "M-Y160M", "卧龙电机"),
            new PartDef("SPU-SENS-001", "温度传感器", "传感器类", "S-PT100", "E+H中国"),
            new PartDef("SPU-PIPE-001", "无缝钢管", "管件类", "P-DN100", "宝钢股份")
    );

    private static final List<RegionDef> REGIONS = List.of(
            new RegionDef("上海", "上海市", List.of("上海生产车间A", "上海中心仓")),
            new RegionDef("江苏", "苏州市", List.of("苏州工业园", "苏州成品仓")),
            new RegionDef("浙江", "杭州市", List.of("杭州生产基地", "杭州石化仓库")),
            new RegionDef("广东", "广州市", List.of("广州电机工厂", "广州仓储中心")),
            new RegionDef("北京", "北京市", List.of("北京分销仓库", "北京物流中心"))
    );

    private final BasePartSpecMapper partSpecMapper;
    private final TraceLifecycleLogMapper logMapper;
    private final TraceSnapshotMapper snapshotMapper;
    private final TraceCodeMapper traceCodeMapper;
    private final SignatureUtil signatureUtil;
    private final TraceDemoDataProperties traceDemoDataProperties;
    private final TraceBatchCommitter batchCommitter;
    private final Random random = new Random(System.currentTimeMillis());

    public TraceDemoDataServiceImpl(
            BasePartSpecMapper partSpecMapper,
            TraceLifecycleLogMapper logMapper,
            TraceSnapshotMapper snapshotMapper,
            TraceCodeMapper traceCodeMapper,
            SignatureUtil signatureUtil,
            TraceDemoDataProperties traceDemoDataProperties,
            TraceBatchCommitter batchCommitter
    ) {
        this.partSpecMapper = partSpecMapper;
        this.logMapper = logMapper;
        this.snapshotMapper = snapshotMapper;
        this.traceCodeMapper = traceCodeMapper;
        this.signatureUtil = signatureUtil;
        this.traceDemoDataProperties = traceDemoDataProperties;
        this.batchCommitter = batchCommitter;
    }

    @Override
    public Map<String, Object> generateSampleData(int count, String operator, String operatorRole) {
        ensureAdminOperationEnabled("generate-sample-data", operator, operatorRole);
        validateGenerateCount(count, operator, operatorRole);
        log.info("Trace demo data generation started: operator={}, role={}, count={}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole), count);

        // Part-spec rows are small + fixed-size; keep their own short transaction.
        List<Long> partIds = createPartSpecsTransactional();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);

        // Stage 1: build all demo trace units in memory (no transaction). Hash + RSA signature
        // happen here so the DB connection is never held while signing.
        List<DemoTraceUnit> units = new ArrayList<>(count);
        int logCount = 0;
        for (int i = 0; i < count; i++) {
            Long spuId = partIds.get(random.nextInt(partIds.size()));
            String traceCode = buildTraceCode(i);
            RegionDef startRegion = REGIONS.get(random.nextInt(REGIONS.size()));
            LocalDateTime eventTime = baseTime.plusDays(i / 5).plusHours(8 + (i % 12));

            DemoTraceUnit unit = buildLifecycleUnit(traceCode, spuId, startRegion, eventTime);
            units.add(unit);
            logCount += unit.logs().size();
        }

        // Stage 2: chunked REQUIRES_NEW commits. Each chunk persists a slice of demo traces;
        // a single trace's logs are never split across chunks (atomic per trace).
        batchCommitter.commitDemoUnitsInChunks(units);

        Map<String, Object> result = new HashMap<>();
        result.put("partSpecs", partIds.size());
        result.put("traceCodes", units.size());
        result.put("lifecycleLogs", logCount);

        log.info("Trace demo data generation completed: operator={}, role={}, count={}, traceCodes={}, lifecycleLogs={}, partSpecs={}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole), count, units.size(), logCount, partIds.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> clearTraceData(String operator, String operatorRole) {
        ensureAdminOperationEnabled("clear-trace-data", operator, operatorRole);
        log.warn("Trace demo data clear started: operator={}, role={}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole));

        long logCount = logMapper.selectCount(null);
        logMapper.delete(null);

        long snapshotCount = snapshotMapper.selectCount(null);
        snapshotMapper.delete(null);

        long traceCodeCount = traceCodeMapper.selectCount(null);
        traceCodeMapper.delete(null);

        Map<String, Object> result = new HashMap<>();
        result.put("deletedLogs", logCount);
        result.put("deletedSnapshots", snapshotCount);
        result.put("deletedTraceCodes", traceCodeCount);

        log.warn("Trace demo data clear completed: operator={}, role={}, deletedLogs={}, deletedSnapshots={}, deletedTraceCodes={}",
                normalizeAuditValue(operator), normalizeAuditValue(operatorRole), logCount, snapshotCount, traceCodeCount);
        return result;
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

    private String normalizeAuditValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String buildTraceCode(int index) {
        String traceCode = "TC-" + LocalDateTime.now().format(TRACE_CODE_DATE) + "-" + String.format("%04d", index + 1);
        if (snapshotMapper.selectById(traceCode) != null) {
            return traceCode + "-" + IdUtil.fastSimpleUUID().substring(0, 4);
        }
        return traceCode;
    }

    @Transactional
    protected List<Long> createPartSpecsTransactional() {
        List<Long> ids = new ArrayList<>();
        Map<String, Long> existingMap = new HashMap<>();
        long syntheticId = -1L;

        for (BasePartSpec part : partSpecMapper.selectList(null)) {
            ids.add(part.getId());
            existingMap.put(part.getPartCode(), part.getId());
        }

        for (PartDef def : PART_DEFINITIONS) {
            if (existingMap.containsKey(def.code())) {
                continue;
            }

            BasePartSpec spec = new BasePartSpec();
            spec.setPartCode(def.code());
            spec.setPartName(def.name());
            spec.setPartType(def.type());
            spec.setModel(def.model());
            spec.setManufacturer(def.manufacturer());
            spec.setUnit("件");
            spec.setRemark("示例数据");
            partSpecMapper.insert(spec);
            if (spec.getId() == null) {
                spec.setId(syntheticId--);
            }
            ids.add(spec.getId());
        }

        if (ids.isEmpty()) {
            ids.add(1L);
        }
        return ids;
    }

    /**
     * Builds (in memory, no transaction) the full lifecycle log chain + tail snapshot for one
     * demo trace. RSA signing is done here, ahead of any commit.
     */
    private DemoTraceUnit buildLifecycleUnit(String traceCode, Long spuId, RegionDef startRegion, LocalDateTime eventTime) {
        List<TraceLifecycleLog> logs = new ArrayList<>();
        String prevHash = GENESIS_HASH;
        String currentNode = startRegion.nodes().get(0);
        TraceStatus currentStatus = TraceStatus.INIT;
        String lastHash;
        LocalDateTime lastEventTime = eventTime;
        RegionDef currentRegion = startRegion;
        ActionType lastAction;

        TraceLifecycleLog initLog = createLog(
                traceCode, spuId, ActionType.INIT,
                null, currentNode,
                startRegion.province(), startRegion.city(),
                eventTime, prevHash, null
        );
        logs.add(initLog);
        prevHash = initLog.getCurrentHash();
        lastHash = initLog.getCurrentHash();
        lastAction = ActionType.INIT;
        currentStatus = TraceStatus.deriveFromAction(currentStatus, ActionType.INIT);

        int totalSteps = 2 + random.nextInt(3);
        for (int step = 0; step < totalSteps; step++) {
            eventTime = eventTime.plusHours(2 + random.nextInt(24));
            ActionType action = getNextLogicalAction(lastAction, step, totalSteps);

            boolean crossRegion = action == ActionType.TRANSFER || (action == ActionType.OUTBOUND && random.nextDouble() < 0.4);
            RegionDef nextRegion = crossRegion ? pickAnotherRegion(currentRegion) : currentRegion;
            String fromNode = currentNode;
            String toNode = action == ActionType.EXCEPTION ? null : nextRegion.nodes().get(random.nextInt(nextRegion.nodes().size()));

            TraceLifecycleLog logEntry = createLog(
                    traceCode, spuId, action,
                    fromNode, toNode,
                    nextRegion.province(), nextRegion.city(),
                    eventTime, prevHash, null
            );
            logs.add(logEntry);

            prevHash = logEntry.getCurrentHash();
            currentNode = toNode != null ? toNode : fromNode;
            currentStatus = TraceStatus.deriveFromAction(currentStatus, action);
            lastHash = logEntry.getCurrentHash();
            lastEventTime = eventTime;
            currentRegion = nextRegion;
            lastAction = action;
        }

        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode(traceCode);
        snapshot.setSpuId(spuId);
        snapshot.setCurrentStatus(currentStatus.getCode());
        snapshot.setCurrentNode(currentNode);
        snapshot.setCurrentOwner(getOperatorForAction(lastAction));
        snapshot.setProvince(ProvinceUtil.toFullName(currentRegion.province()));
        snapshot.setCity(currentRegion.city());
        snapshot.setLastEventTime(lastEventTime);
        snapshot.setLastHash(lastHash);
        snapshot.setVersion(0);
        // lastLogId is filled by TraceBatchCommitter after the final log INSERT assigns its PK.

        return DemoTraceUnit.of(logs, snapshot);
    }

    private RegionDef pickAnotherRegion(RegionDef currentRegion) {
        RegionDef nextRegion = REGIONS.get(random.nextInt(REGIONS.size()));
        if (REGIONS.size() > 1 && nextRegion.equals(currentRegion)) {
            nextRegion = REGIONS.get((REGIONS.indexOf(currentRegion) + 1) % REGIONS.size());
        }
        return nextRegion;
    }

    private TraceLifecycleLog createLog(
            String traceCode,
            Long spuId,
            ActionType actionType,
            String fromNode,
            String toNode,
            String province,
            String city,
            LocalDateTime eventTime,
            String prevHash,
            Long correctionOf
    ) {
        LocalDateTime ingestTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime truncatedEventTime = eventTime.truncatedTo(ChronoUnit.SECONDS);
        String fullProvince = ProvinceUtil.toFullName(province);
        String operator = getOperatorForAction(actionType);

        String currentHash = HashUtil.calculateHash(
                traceCode, actionType.name(), fromNode, toNode,
                fullProvince, city, null, truncatedEventTime, ingestTime, prevHash, correctionOf, operator
        );
        String signature = signatureUtil.sign(SignatureUtil.buildSignatureData(
                traceCode, actionType.name(), fromNode, toNode,
                fullProvince, city,
                truncatedEventTime.toString(), ingestTime.toString(),
                prevHash, currentHash, correctionOf, operator, null
        ));

        TraceLifecycleLog logEntry = new TraceLifecycleLog();
        logEntry.setTraceCode(traceCode);
        logEntry.setSpuId(spuId);
        logEntry.setActionType(actionType.name());
        logEntry.setFromNode(fromNode);
        logEntry.setToNode(toNode);
        logEntry.setProvince(fullProvince);
        logEntry.setCity(city);
        logEntry.setRemark(null);
        logEntry.setEventTime(truncatedEventTime);
        logEntry.setIngestTime(ingestTime);
        logEntry.setPrevHash(prevHash);
        logEntry.setCurrentHash(currentHash);
        logEntry.setCorrectionOf(correctionOf);
        logEntry.setOperator(operator);
        logEntry.setSignatureKeyId(signatureUtil.getKeyId());
        logEntry.setSignatureKeyVersion(signatureUtil.getKeyVersion());
        logEntry.setSignature(signature);
        return logEntry;
    }

    private ActionType getNextLogicalAction(ActionType lastAction, int step, int totalSteps) {
        boolean isLastStep = step == totalSteps - 1;
        if (lastAction == null || lastAction == ActionType.INIT) {
            return ActionType.INBOUND;
        }

        return switch (lastAction) {
            case INBOUND -> random.nextDouble() < 0.08 ? ActionType.EXCEPTION : ActionType.OUTBOUND;
            case OUTBOUND -> ActionType.TRANSFER;
            case TRANSFER -> ActionType.INBOUND;
            case EXCEPTION -> random.nextDouble() < 0.5 ? ActionType.CORRECTION : ActionType.INBOUND;
            case CORRECTION -> isLastStep ? ActionType.INBOUND : ActionType.OUTBOUND;
            default -> ActionType.INBOUND;
        };
    }

    private String getOperatorForAction(ActionType action) {
        if (action == null) {
            return "system";
        }
        return switch (action) {
            case INIT, PRINT_CODE, REPRINT_CODE, ACTIVATE_CODE, VOID_CODE,
                    PACK, UNPACK, PALLETIZE, UNPALLETIZE -> "producer";
            case INBOUND, OUTBOUND -> "warehouse";
            case TRANSFER -> "logistics";
            case EXCEPTION, CORRECTION -> "warehouse";
        };
    }

    private record PartDef(String code, String name, String type, String model, String manufacturer) {}

    private record RegionDef(String province, String city, List<String> nodes) {}
}
