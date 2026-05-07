package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceFlowTaskCompleteRequest;
import com.example.trace.dto.TraceFlowTaskCreateRequest;
import com.example.trace.dto.TraceFlowTaskResponse;
import com.example.trace.dto.TraceFlowTaskScanRequest;
import com.example.trace.entity.TraceAggregation;
import com.example.trace.entity.TraceFlowTask;
import com.example.trace.entity.TraceFlowTaskScan;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceFlowTaskDiscrepancyType;
import com.example.trace.enums.TraceFlowTaskStatus;
import com.example.trace.enums.TraceFlowTaskType;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceFlowTaskMapper;
import com.example.trace.mapper.TraceFlowTaskScanMapper;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.TraceFlowTaskService;
import com.example.trace.service.impl.support.TraceScanRetryExecutor;
import com.example.trace.validation.TraceLocationFieldConstraints;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TraceFlowTaskServiceImpl implements TraceFlowTaskService {

    private static final Pattern TASK_NO = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");
    private static final DateTimeFormatter TASK_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_AGGREGATION_EXPANSION_DEPTH = 8;

    private final TraceAggregationMapper traceAggregationMapper;
    private final TraceFlowTaskMapper traceFlowTaskMapper;
    private final TraceFlowTaskScanMapper traceFlowTaskScanMapper;
    private final TraceNodeMapper traceNodeMapper;
    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceScanRetryExecutor traceScanRetryExecutor;

    @Override
    public List<TraceFlowTaskResponse> listTasks(TraceFlowTaskType taskType, TraceFlowTaskStatus status) {
        LambdaQueryWrapper<TraceFlowTask> wrapper = new LambdaQueryWrapper<>();
        if (taskType != null) {
            wrapper.eq(TraceFlowTask::getTaskType, taskType.getCode());
        }
        if (status != null) {
            wrapper.eq(TraceFlowTask::getStatus, status.getCode());
        }
        wrapper.orderByDesc(TraceFlowTask::getCreateTime)
                .orderByDesc(TraceFlowTask::getId);
        return toResponses(traceFlowTaskMapper.selectList(wrapper));
    }

    @Override
    public TraceFlowTaskResponse getTaskById(Long id) {
        return toResponse(requireTask(id));
    }

    @Override
    public TraceFlowTaskResponse getTaskByNo(String taskNo) {
        String normalizedTaskNo = normalizeTaskNo(taskNo, true);
        TraceFlowTask task = traceFlowTaskMapper.selectByTaskNo(normalizedTaskNo);
        if (task == null) {
            throw new BizException(BizCode.NOT_FOUND, "流转任务不存在: " + normalizedTaskNo);
        }
        return toResponse(task);
    }

    @Override
    @Transactional
    public TraceFlowTaskResponse createTask(
            TraceFlowTaskCreateRequest request,
            Long operatorUserId,
            String operatorUsername
    ) {
        if (request == null) {
            throw new BizException(BizCode.PARAM_ERROR, "流转任务创建参数不能为空");
        }
        TraceFlowTaskType taskType = requireTaskType(request.getTaskType());
        String taskNo = normalizeTaskNoOrGenerate(request.getTaskNo());
        if (traceFlowTaskMapper.selectByTaskNo(taskNo) != null) {
            throw new BizException(BizCode.CONFLICT, "流转任务号已存在: " + taskNo);
        }

        TraceNode sourceNode = requireEnabledNode(request.getSourceNodeId(), "sourceNodeId");
        TraceNode targetNode = requireEnabledNode(request.getTargetNodeId(), "targetNodeId");
        if (Objects.equals(sourceNode.getId(), targetNode.getId())) {
            throw new BizException(BizCode.PARAM_ERROR, "sourceNodeId 和 targetNodeId 不能相同");
        }
        int expectedQuantity = requireExpectedQuantity(request.getExpectedQuantity());

        TraceFlowTask task = new TraceFlowTask();
        task.setTaskNo(taskNo);
        task.setTaskType(taskType.getCode());
        task.setSourceNodeId(sourceNode.getId());
        task.setTargetNodeId(targetNode.getId());
        task.setExpectedQuantity(expectedQuantity);
        task.setActualQuantity(0);
        task.setStatus(TraceFlowTaskStatus.CREATED.getCode());
        task.setDiscrepancyType(TraceFlowTaskDiscrepancyType.NONE.getCode());
        task.setDiscrepancyQuantity(0);
        task.setCreateBy(operatorUserId);
        task.setCreateByUsername(normalizeUsername(operatorUsername));
        task.setRemark(normalizeRemark(request.getRemark()));
        traceFlowTaskMapper.insert(task);
        return toResponse(task, sourceNode, targetNode);
    }

    @Override
    @Transactional
    public TraceFlowTaskResponse cancelTask(Long id) {
        TraceFlowTask task = requireTask(id);
        TraceFlowTaskStatus status = parseStatus(task);
        if (status.terminal()) {
            throw new BizException(BizCode.BAD_REQUEST, "终态流转任务不能取消: status=" + status.getCode());
        }
        task.setStatus(TraceFlowTaskStatus.CANCELLED.getCode());
        task.setCancelTime(nowSeconds());
        traceFlowTaskMapper.updateById(task);
        return toResponse(task);
    }

    @Override
    @Transactional
    public TraceFlowTaskResponse completeTask(Long id, TraceFlowTaskCompleteRequest request) {
        TraceFlowTask task = requireTask(id);
        TraceFlowTaskStatus status = parseStatus(task);
        if (status.terminal()) {
            throw new BizException(BizCode.BAD_REQUEST, "终态流转任务不能重复完成: status=" + status.getCode());
        }
        int expectedQuantity = requireExpectedQuantity(task.getExpectedQuantity());
        int actualQuantity = request != null && request.getActualQuantity() != null
                ? requireActualQuantity(request.getActualQuantity())
                : requireActualQuantity(task.getActualQuantity() == null ? 0 : task.getActualQuantity());
        TraceFlowTaskDiscrepancyType discrepancyType = resolveDiscrepancyType(expectedQuantity, actualQuantity);
        String remark = request != null && request.getRemark() != null
                ? normalizeRemark(request.getRemark())
                : task.getRemark();
        String discrepancyReason = resolveDiscrepancyReason(request);

        task.setActualQuantity(actualQuantity);
        task.setRemark(remark);
        if (discrepancyType == TraceFlowTaskDiscrepancyType.NONE) {
            task.setStatus(TraceFlowTaskStatus.COMPLETED.getCode());
            task.setCompleteTime(nowSeconds());
            task.setDiscrepancyType(TraceFlowTaskDiscrepancyType.NONE.getCode());
            task.setDiscrepancyQuantity(0);
            task.setDiscrepancyReason(null);
            task.setDiscrepancyTime(null);
        } else {
            if (discrepancyReason == null) {
                throw new BizException(BizCode.PARAM_ERROR,
                        "任务实扫数量与预计数量不一致，必须填写差异原因: expectedQuantity="
                                + expectedQuantity + ", actualQuantity=" + actualQuantity);
            }
            task.setStatus(TraceFlowTaskStatus.EXCEPTION.getCode());
            task.setDiscrepancyType(discrepancyType.getCode());
            task.setDiscrepancyQuantity(Math.abs(actualQuantity - expectedQuantity));
            task.setDiscrepancyReason(discrepancyReason);
            task.setDiscrepancyTime(nowSeconds());
        }
        traceFlowTaskMapper.updateById(task);
        return toResponse(task);
    }

    @Override
    @Transactional
    public TraceFlowTaskResponse scanTask(
            Long id,
            TraceFlowTaskScanRequest request,
            Long operatorUserId,
            String operatorUsername
    ) {
        if (request == null) {
            throw new BizException(BizCode.PARAM_ERROR, "流转任务扫码参数不能为空");
        }

        TraceFlowTask task = requireTask(id);
        ensureTaskOpen(task);

        TraceNode sourceNode = requireEnabledNode(task.getSourceNodeId(), "sourceNodeId");
        TraceNode targetNode = requireEnabledNode(task.getTargetNodeId(), "targetNodeId");
        String scannedCode = normalizeTraceCode(request.getTraceCode());
        TaskScanTargets scanTargets = resolveTaskScanTargets(scannedCode);
        if (scanTargets.batchScan()) {
            return scanTaskBatch(
                    task,
                    sourceNode,
                    targetNode,
                    scanTargets,
                    request,
                    operatorUserId,
                    operatorUsername
            );
        }

        String traceCode = scanTargets.childTraceCodes().get(0);
        TraceSnapshot snapshot = requireSnapshot(traceCode);
        TraceFlowTaskScan latestTaskScan = traceFlowTaskScanMapper.selectLatestByTaskTrace(task.getId(), traceCode);
        TaskScanPlan scanPlan;
        try {
            scanPlan = resolveTaskScanPlan(task, sourceNode, targetNode, traceCode, snapshot);
        } catch (BizException e) {
            if (latestTaskScan != null) {
                return duplicateScanResponse(task, sourceNode, targetNode, latestTaskScan);
            }
            throw e;
        }
        TraceFlowTaskScan existingScan = traceFlowTaskScanMapper.selectByTaskTraceAction(
                task.getId(),
                traceCode,
                scanPlan.actionType().getCode()
        );
        if (existingScan != null) {
            return duplicateScanResponse(task, sourceNode, targetNode, existingScan);
        }
        if (scanPlan.countsTowardsActualQuantity()) {
            ensureTaskCapacity(task);
        }
        String taskScanIdempotencyKey = resolveTaskScanIdempotencyKey(
                task,
                traceCode,
                scanPlan.actionType(),
                request.getIdempotencyKey()
        );

        ScanTraceRequest scanRequest = buildScanTraceRequest(
                task,
                request,
                traceCode,
                scanPlan,
                operatorUserId,
                taskScanIdempotencyKey
        );

        boolean created = traceScanRetryExecutor.executeAndReturnCreated(scanRequest, operatorForLog(operatorUsername));
        if (!created) {
            TraceFlowTaskResponse response = toResponse(requireTask(id));
            applyScanFeedback(
                    response,
                    traceCode,
                    scanPlan.actionType(),
                    false,
                    true,
                    "该码已在当前任务内扫码处理，不重复计数"
            );
            return response;
        }

        insertTaskScanRecord(
                task,
                traceCode,
                scanPlan.actionType(),
                scanPlan.countsTowardsActualQuantity(),
                operatorUserId,
                operatorUsername,
                taskScanIdempotencyKey
        );
        if (scanPlan.countsTowardsActualQuantity()) {
            task.setActualQuantity((task.getActualQuantity() == null ? 0 : task.getActualQuantity()) + 1);
        }
        task.setStatus(TraceFlowTaskStatus.PROCESSING.getCode());
        traceFlowTaskMapper.updateById(task);
        TraceFlowTaskResponse response = toResponse(task, sourceNode, targetNode);
        applyScanFeedback(
                response,
                traceCode,
                scanPlan.actionType(),
                true,
                false,
                scanPlan.countsTowardsActualQuantity()
                        ? "扫码成功，已累计到任务实扫数量"
                        : "扫码成功，已记录接收确认"
        );
        return response;
    }

    private TraceFlowTaskResponse scanTaskBatch(
            TraceFlowTask task,
            TraceNode sourceNode,
            TraceNode targetNode,
            TaskScanTargets scanTargets,
            TraceFlowTaskScanRequest request,
            Long operatorUserId,
            String operatorUsername
    ) {
        List<BatchChildScanPlan> childPlans = new ArrayList<>();
        List<TraceFlowTaskScan> duplicateScans = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (String childTraceCode : scanTargets.childTraceCodes()) {
            TraceFlowTaskScan latestTaskScan = traceFlowTaskScanMapper.selectLatestByTaskTrace(
                    task.getId(),
                    childTraceCode
            );
            TraceSnapshot snapshot = traceSnapshotMapper.selectById(childTraceCode);
            if (snapshot == null) {
                if (latestTaskScan != null) {
                    duplicateScans.add(latestTaskScan);
                    continue;
                }
                failures.add(childTraceCode + ": 单品溯源码快照不存在");
                continue;
            }

            TaskScanPlan scanPlan;
            try {
                scanPlan = resolveTaskScanPlan(task, sourceNode, targetNode, childTraceCode, snapshot);
            } catch (BizException e) {
                if (latestTaskScan != null) {
                    duplicateScans.add(latestTaskScan);
                    continue;
                }
                failures.add(childTraceCode + ": " + e.getMessage());
                continue;
            }

            TraceFlowTaskScan existingScan = traceFlowTaskScanMapper.selectByTaskTraceAction(
                    task.getId(),
                    childTraceCode,
                    scanPlan.actionType().getCode()
            );
            if (existingScan != null) {
                duplicateScans.add(existingScan);
                continue;
            }
            childPlans.add(new BatchChildScanPlan(childTraceCode, scanPlan));
        }

        if (!failures.isEmpty()) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "父码 " + scanTargets.parentCode()
                            + " 内存在不可流转子码，已拒绝整批扫描: "
                            + summarizeBatchFailures(failures));
        }

        ensureUniformBatchAction(scanTargets.parentCode(), childPlans);
        int countedToCreate = (int) childPlans.stream()
                .filter(childPlan -> childPlan.scanPlan().countsTowardsActualQuantity())
                .count();
        ensureTaskBatchCapacity(task, countedToCreate, scanTargets.parentCode());

        duplicateScans.forEach(scan -> {
            if (scan.getId() != null) {
                traceFlowTaskScanMapper.incrementDuplicateCount(scan.getId());
            }
        });

        int createdQuantity = 0;
        int countedCreatedQuantity = 0;
        int idempotentSkippedQuantity = 0;
        for (BatchChildScanPlan childPlan : childPlans) {
            TaskScanPlan scanPlan = childPlan.scanPlan();
            String taskScanIdempotencyKey = resolveTaskScanIdempotencyKey(
                    task,
                    childPlan.traceCode(),
                    scanPlan.actionType(),
                    request.getIdempotencyKey()
            );
            ScanTraceRequest scanRequest = buildScanTraceRequest(
                    task,
                    request,
                    childPlan.traceCode(),
                    scanPlan,
                    operatorUserId,
                    taskScanIdempotencyKey
            );
            boolean created = traceScanRetryExecutor.executeAndReturnCreated(
                    scanRequest,
                    operatorForLog(operatorUsername)
            );
            if (!created) {
                idempotentSkippedQuantity++;
                continue;
            }

            insertTaskScanRecord(
                    task,
                    childPlan.traceCode(),
                    scanPlan.actionType(),
                    scanPlan.countsTowardsActualQuantity(),
                    operatorUserId,
                    operatorUsername,
                    taskScanIdempotencyKey
            );
            createdQuantity++;
            if (scanPlan.countsTowardsActualQuantity()) {
                countedCreatedQuantity++;
            }
        }

        if (createdQuantity > 0) {
            task.setActualQuantity((task.getActualQuantity() == null ? 0 : task.getActualQuantity())
                    + countedCreatedQuantity);
            task.setStatus(TraceFlowTaskStatus.PROCESSING.getCode());
            traceFlowTaskMapper.updateById(task);
        }

        TraceFlowTaskResponse response = toResponse(task, sourceNode, targetNode);
        applyBatchScanFeedback(
                response,
                scanTargets,
                resolveBatchFeedbackAction(childPlans, duplicateScans),
                createdQuantity,
                countedCreatedQuantity,
                duplicateScans.size(),
                idempotentSkippedQuantity
        );
        return response;
    }

    private TraceFlowTask requireTask(Long id) {
        if (id == null) {
            throw new BizException(BizCode.PARAM_ERROR, "taskId 不能为空");
        }
        TraceFlowTask task = traceFlowTaskMapper.selectById(id);
        if (task == null) {
            throw new BizException(BizCode.NOT_FOUND, "流转任务不存在: " + id);
        }
        return task;
    }

    private TraceNode requireEnabledNode(Long nodeId, String fieldName) {
        if (nodeId == null) {
            throw new BizException(BizCode.PARAM_ERROR, fieldName + " 不能为空");
        }
        TraceNode node = traceNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BizException(BizCode.NOT_FOUND, "节点不存在: " + nodeId);
        }
        if (!Boolean.TRUE.equals(node.getEnabled())) {
            throw new BizException(BizCode.BAD_REQUEST, "节点已停用，不能用于流转任务: " + nodeId);
        }
        return node;
    }

    private void ensureTaskOpen(TraceFlowTask task) {
        TraceFlowTaskStatus status = parseStatus(task);
        if (status != TraceFlowTaskStatus.CREATED && status != TraceFlowTaskStatus.PROCESSING) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "当前流转任务状态不允许扫码: status=" + status.getCode());
        }
        int expectedQuantity = task.getExpectedQuantity() == null ? 0 : task.getExpectedQuantity();
        if (expectedQuantity <= 0) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "流转任务预计数量非法: taskId=" + task.getId() + ", expectedQuantity=" + expectedQuantity);
        }
    }

    private void ensureTaskCapacity(TraceFlowTask task) {
        int expectedQuantity = task.getExpectedQuantity() == null ? 0 : task.getExpectedQuantity();
        int actualQuantity = task.getActualQuantity() == null ? 0 : task.getActualQuantity();
        if (actualQuantity >= expectedQuantity) {
            throw new BizException(BizCode.CONFLICT,
                    "流转任务扫码数量已达到预计数量: taskId=" + task.getId());
        }
    }

    private TraceFlowTaskDiscrepancyType resolveDiscrepancyType(int expectedQuantity, int actualQuantity) {
        if (actualQuantity == expectedQuantity) {
            return TraceFlowTaskDiscrepancyType.NONE;
        }
        return actualQuantity < expectedQuantity
                ? TraceFlowTaskDiscrepancyType.SHORTAGE
                : TraceFlowTaskDiscrepancyType.OVERAGE;
    }

    private String resolveDiscrepancyReason(TraceFlowTaskCompleteRequest request) {
        String reason = request == null ? null : normalizeRemark(request.getDiscrepancyReason());
        if (reason != null) {
            return reason;
        }
        return request == null ? null : normalizeRemark(request.getRemark());
    }

    private void insertTaskScanRecord(
            TraceFlowTask task,
            String traceCode,
            ActionType actionType,
            boolean counted,
            Long operatorUserId,
            String operatorUsername,
            String idempotencyKey
    ) {
        TraceFlowTaskScan scan = new TraceFlowTaskScan();
        scan.setTaskId(task.getId());
        scan.setTraceCode(traceCode);
        scan.setActionType(actionType.getCode());
        scan.setCounted(counted);
        scan.setOperatorUserId(operatorUserId);
        scan.setOperatorUsername(normalizeUsername(operatorUsername));
        scan.setIdempotencyKey(idempotencyKey);
        scan.setScanTime(nowSeconds());
        scan.setDuplicateCount(0);
        try {
            traceFlowTaskScanMapper.insert(scan);
        } catch (DuplicateKeyException e) {
            TraceFlowTaskScan existing = traceFlowTaskScanMapper.selectByTaskTraceAction(
                    task.getId(),
                    traceCode,
                    actionType.getCode()
            );
            if (existing != null) {
                traceFlowTaskScanMapper.incrementDuplicateCount(existing.getId());
                throw new BizException(BizCode.CONFLICT,
                        "该码已在当前任务内扫码，不重复计数: traceCode=" + traceCode);
            }
            throw e;
        }
    }

    private TraceFlowTaskResponse duplicateScanResponse(
            TraceFlowTask task,
            TraceNode sourceNode,
            TraceNode targetNode,
            TraceFlowTaskScan existingScan
    ) {
        if (existingScan.getId() != null) {
            traceFlowTaskScanMapper.incrementDuplicateCount(existingScan.getId());
        }
        TraceFlowTaskResponse response = toResponse(task, sourceNode, targetNode);
        applyScanFeedback(
                response,
                existingScan.getTraceCode(),
                parseActionType(existingScan),
                false,
                true,
                "该码已在当前任务内扫码，不重复计数"
        );
        return response;
    }

    private ActionType parseActionType(TraceFlowTaskScan scan) {
        try {
            return ActionType.fromString(scan.getActionType());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "任务扫码记录动作类型非法: scanId=" + scan.getId()
                            + ", actionType=" + scan.getActionType());
        }
    }

    private void applyScanFeedback(
            TraceFlowTaskResponse response,
            String traceCode,
            ActionType actionType,
            boolean created,
            boolean duplicate,
            String message
    ) {
        response.setLastScanTraceCode(traceCode);
        response.setLastScanActionType(actionType);
        response.setLastScanActionLabel(actionType.getName());
        response.setLastScanCreated(created);
        response.setDuplicateScan(duplicate);
        response.setScanMessage(message);
        response.setBatchScan(false);
    }

    private void applyBatchScanFeedback(
            TraceFlowTaskResponse response,
            TaskScanTargets scanTargets,
            ActionType actionType,
            int createdQuantity,
            int countedCreatedQuantity,
            int duplicateQuantity,
            int skippedQuantity
    ) {
        response.setLastScanTraceCode(scanTargets.parentCode());
        response.setLastScanActionType(actionType);
        response.setLastScanActionLabel(actionType == null ? null : actionType.getName());
        response.setLastScanCreated(createdQuantity > 0);
        response.setDuplicateScan(createdQuantity == 0);
        response.setBatchScan(true);
        response.setBatchParentCode(scanTargets.parentCode());
        response.setBatchExpandedQuantity(scanTargets.childTraceCodes().size());
        response.setBatchCreatedQuantity(createdQuantity);
        response.setBatchDuplicateQuantity(duplicateQuantity);
        response.setBatchSkippedQuantity(skippedQuantity);

        String message = "父码 " + scanTargets.parentCode()
                + " 展开 " + scanTargets.childTraceCodes().size() + " 个单品码"
                + "，新增 " + createdQuantity + " 个"
                + "，重复 " + duplicateQuantity + " 个"
                + "，跳过 " + skippedQuantity + " 个";
        if (countedCreatedQuantity > 0) {
            message += "，本次累计 " + countedCreatedQuantity + " 件";
        }
        if (createdQuantity == 0) {
            message += "，不重复计数";
        }
        response.setScanMessage(message);
    }

    private ScanTraceRequest buildScanTraceRequest(
            TraceFlowTask task,
            TraceFlowTaskScanRequest request,
            String traceCode,
            TaskScanPlan scanPlan,
            Long operatorUserId,
            String idempotencyKey
    ) {
        ScanTraceRequest scanRequest = new ScanTraceRequest();
        scanRequest.setTraceCode(traceCode);
        scanRequest.setOperatorUserId(operatorUserId);
        scanRequest.setActionType(scanPlan.actionType());
        scanRequest.setFromNode(scanPlan.fromNode());
        scanRequest.setToNode(scanPlan.toNode());
        scanRequest.setProvince(scanPlan.operationNode().getProvince());
        scanRequest.setCity(scanPlan.operationNode().getCity());
        scanRequest.setEventTime(request.getEventTime());
        scanRequest.setIdempotencyKey(idempotencyKey);
        scanRequest.setRemark(resolveTaskScanRemark(task, request.getRemark(), scanPlan.actionType()));
        return scanRequest;
    }

    private TaskScanTargets resolveTaskScanTargets(String scannedCode) {
        String aggregationParentCode = scannedCode.toUpperCase(Locale.ROOT);
        if (selectActiveChildren(aggregationParentCode).isEmpty()) {
            return new TaskScanTargets(null, false, List.of(scannedCode));
        }
        LinkedHashSet<String> childTraceCodes = new LinkedHashSet<>();
        expandActiveLeafChildren(
                aggregationParentCode,
                childTraceCodes,
                new HashSet<>(),
                0
        );
        if (childTraceCodes.isEmpty()) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "聚合父码没有可流转单品子码: parentCode=" + aggregationParentCode);
        }
        return new TaskScanTargets(
                aggregationParentCode,
                true,
                List.copyOf(childTraceCodes)
        );
    }

    private void expandActiveLeafChildren(
            String parentCode,
            LinkedHashSet<String> childTraceCodes,
            Set<String> visitingParents,
            int depth
    ) {
        if (depth > MAX_AGGREGATION_EXPANSION_DEPTH) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "聚合层级过深，无法展开父码: parentCode=" + parentCode);
        }
        String normalizedParentCode = parentCode.toUpperCase(Locale.ROOT);
        if (!visitingParents.add(normalizedParentCode)) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "检测到聚合关系循环，无法展开父码: parentCode=" + normalizedParentCode);
        }
        for (TraceAggregation relation : selectActiveChildren(normalizedParentCode)) {
            String childCode = normalizeTraceCode(relation.getChildCode());
            String childAsParentCode = childCode.toUpperCase(Locale.ROOT);
            if (selectActiveChildren(childAsParentCode).isEmpty()) {
                childTraceCodes.add(childCode);
            } else {
                expandActiveLeafChildren(childAsParentCode, childTraceCodes, visitingParents, depth + 1);
            }
        }
        visitingParents.remove(normalizedParentCode);
    }

    private List<TraceAggregation> selectActiveChildren(String parentCode) {
        List<TraceAggregation> children = traceAggregationMapper.selectActiveChildrenByParent(parentCode);
        return children == null ? List.of() : children;
    }

    private void ensureUniformBatchAction(String parentCode, List<BatchChildScanPlan> childPlans) {
        Set<ActionType> actionTypes = childPlans.stream()
                .map(BatchChildScanPlan::scanPlan)
                .map(TaskScanPlan::actionType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (actionTypes.size() <= 1) {
            return;
        }
        throw new BizException(BizCode.INVALID_ACTION_TYPE,
                "父码 " + parentCode
                        + " 内子码可执行动作不一致，已拒绝整批扫描: actions="
                        + actionTypes.stream().map(ActionType::getCode).toList());
    }

    private void ensureTaskBatchCapacity(TraceFlowTask task, int countedToCreate, String parentCode) {
        if (countedToCreate <= 0) {
            return;
        }
        int expectedQuantity = task.getExpectedQuantity() == null ? 0 : task.getExpectedQuantity();
        int actualQuantity = task.getActualQuantity() == null ? 0 : task.getActualQuantity();
        int remainingQuantity = expectedQuantity - actualQuantity;
        if (countedToCreate > remainingQuantity) {
            throw new BizException(BizCode.CONFLICT,
                    "父码 " + parentCode
                            + " 展开后将新增 " + countedToCreate
                            + " 件，超过任务剩余容量 " + Math.max(remainingQuantity, 0));
        }
    }

    private ActionType resolveBatchFeedbackAction(
            List<BatchChildScanPlan> childPlans,
            List<TraceFlowTaskScan> duplicateScans
    ) {
        if (!childPlans.isEmpty()) {
            return childPlans.get(0).scanPlan().actionType();
        }
        if (!duplicateScans.isEmpty()) {
            return parseActionType(duplicateScans.get(0));
        }
        return null;
    }

    private String summarizeBatchFailures(List<String> failures) {
        String summary = failures.stream()
                .limit(5)
                .collect(Collectors.joining("; "));
        if (failures.size() > 5) {
            summary += "; 等 " + failures.size() + " 个异常子码";
        }
        return summary;
    }

    private TraceSnapshot requireSnapshot(String traceCode) {
        TraceSnapshot snapshot = traceSnapshotMapper.selectById(traceCode);
        if (snapshot == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND, "未知溯源码: " + traceCode);
        }
        return snapshot;
    }

    private TaskScanPlan resolveTaskScanPlan(
            TraceFlowTask task,
            TraceNode sourceNode,
            TraceNode targetNode,
            String traceCode,
            TraceSnapshot snapshot
    ) {
        TraceFlowTaskType taskType = parseTaskType(task);
        TraceStatus currentStatus = parseSnapshotStatus(traceCode, snapshot);
        return switch (taskType) {
            case OUTBOUND -> resolveOutboundTaskScan(task, sourceNode, targetNode, traceCode, snapshot, currentStatus);
            case INBOUND, RECEIVE -> resolveReceiveTaskScan(targetNode, traceCode, snapshot, currentStatus, true);
            case TRANSFER -> throw new BizException(BizCode.BAD_REQUEST,
                    "B18 暂不支持 TRANSFER 任务扫码；请使用 OUTBOUND + RECEIVE/INBOUND 形成接收闭环");
        };
    }

    private TaskScanPlan resolveOutboundTaskScan(
            TraceFlowTask task,
            TraceNode sourceNode,
            TraceNode targetNode,
            String traceCode,
            TraceSnapshot snapshot,
            TraceStatus currentStatus
    ) {
        if (currentStatus == TraceStatus.IN_STOCK) {
            String currentNode = requireNodeMatch(
                    traceCode,
                    snapshot,
                    sourceNode,
                    "出库任务来源节点",
                    "只有当前节点库存内物品可加入出库任务"
            );
            return new TaskScanPlan(
                    ActionType.OUTBOUND,
                    currentNode,
                    targetNode.getNodeName(),
                    sourceNode,
                    true
            );
        }
        if (currentStatus == TraceStatus.IN_TRANSIT || currentStatus == TraceStatus.TRANSFERRED) {
            if ((task.getActualQuantity() == null ? 0 : task.getActualQuantity()) <= 0) {
                throw new BizException(BizCode.BAD_REQUEST,
                        "出库任务尚未记录任何出库扫码，不能直接接收: taskId=" + task.getId());
            }
            return resolveReceiveTaskScan(targetNode, traceCode, snapshot, currentStatus, false);
        }
        throw new BizException(BizCode.INVALID_ACTION_TYPE,
                "只有在库或运输中的物品可在出库任务内扫码: traceCode="
                        + traceCode + ", currentStatus=" + currentStatus.getCode());
    }

    private TaskScanPlan resolveReceiveTaskScan(
            TraceNode targetNode,
            String traceCode,
            TraceSnapshot snapshot,
            TraceStatus currentStatus,
            boolean countsTowardsActualQuantity
    ) {
        if (currentStatus != TraceStatus.IN_TRANSIT && currentStatus != TraceStatus.TRANSFERRED) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "只有运输中物品允许目标节点接收: traceCode="
                            + traceCode + ", currentStatus=" + currentStatus.getCode());
        }
        String currentNode = requireNodeMatch(
                traceCode,
                snapshot,
                targetNode,
                "接收任务目标节点",
                "货物不在接收任务目标节点"
        );
        return new TaskScanPlan(
                ActionType.INBOUND,
                currentNode,
                targetNode.getNodeName(),
                targetNode,
                countsTowardsActualQuantity
        );
    }

    private TraceStatus parseSnapshotStatus(String traceCode, TraceSnapshot snapshot) {
        TraceStatus currentStatus;
        try {
            currentStatus = TraceStatus.fromString(snapshot.getCurrentStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE,
                    "溯源码当前状态非法，无法执行流转任务: traceCode="
                            + traceCode + ", currentStatus=" + snapshot.getCurrentStatus());
        }
        return currentStatus;
    }

    private String requireNodeMatch(
            String traceCode,
            TraceSnapshot snapshot,
            TraceNode expectedNode,
            String expectedLabel,
            String message
    ) {
        String currentNode = TraceLocationFieldConstraints.normalizeNode("currentNode", snapshot.getCurrentNode());
        if (!matchesNode(expectedNode, currentNode)) {
            throw new BizException(BizCode.BAD_REQUEST,
                    message + ": traceCode="
                            + traceCode
                            + ", expected" + expectedLabel + "=" + expectedNode.getNodeName()
                            + ", currentNode=" + currentNode);
        }
        return currentNode;
    }

    private boolean matchesNode(TraceNode node, String currentNode) {
        if (node == null || !StringUtils.hasText(currentNode)) {
            return false;
        }
        if (node.getNodeName() != null && node.getNodeName().equals(currentNode)) {
            return true;
        }
        return node.getNodeCode() != null && node.getNodeCode().equalsIgnoreCase(currentNode);
    }

    private TraceFlowTaskType requireTaskType(TraceFlowTaskType taskType) {
        if (taskType == null) {
            throw new BizException(BizCode.PARAM_ERROR, "taskType 不能为空");
        }
        return taskType;
    }

    private int requireExpectedQuantity(Integer value) {
        if (value == null || value < 1 || value > 100000) {
            throw new BizException(BizCode.PARAM_ERROR, "expectedQuantity 必须在 1 到 100000 之间");
        }
        return value;
    }

    private int requireActualQuantity(Integer value) {
        if (value == null || value < 0 || value > 100000) {
            throw new BizException(BizCode.PARAM_ERROR, "actualQuantity 必须在 0 到 100000 之间");
        }
        return value;
    }

    private String normalizeTaskNoOrGenerate(String value) {
        if (!StringUtils.hasText(value)) {
            return "FLOW-" + TASK_NO_TIME.format(nowSeconds())
                    + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return normalizeTaskNo(value, true);
    }

    private String normalizeTaskNo(String value, boolean required) {
        if (!StringUtils.hasText(value)) {
            if (required) {
                throw new BizException(BizCode.PARAM_ERROR, "taskNo 不能为空");
            }
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.length() > 64 || !TASK_NO.matcher(normalized).matches()) {
            throw new BizException(BizCode.PARAM_ERROR, "taskNo 格式不合法");
        }
        return normalized;
    }

    private String normalizeUsername(String operatorUsername) {
        if (!StringUtils.hasText(operatorUsername)) {
            return null;
        }
        String normalized = operatorUsername.trim();
        if (normalized.length() > 64) {
            return normalized.substring(0, 64);
        }
        return normalized;
    }

    private String operatorForLog(String operatorUsername) {
        String normalized = normalizeUsername(operatorUsername);
        return normalized == null ? "unknown" : normalized;
    }

    private String normalizeTraceCode(String traceCode) {
        String normalized = TraceLocationFieldConstraints.normalizeIdempotencyKey("traceCode", traceCode);
        if (normalized == null) {
            throw new BizException(BizCode.PARAM_ERROR, "traceCode 不能为空");
        }
        return normalized;
    }

    private String normalizeRemark(String remark) {
        return TraceLocationFieldConstraints.normalizeRemark("remark", remark);
    }

    private String resolveTaskScanRemark(TraceFlowTask task, String remark, ActionType actionType) {
        String normalized = normalizeRemark(remark);
        if (normalized != null) {
            return normalized;
        }
        String label = actionType == ActionType.INBOUND ? "任务接收" : "任务出库";
        return TraceLocationFieldConstraints.normalizeRemark("remark", label + ": " + task.getTaskNo());
    }

    private String resolveTaskScanIdempotencyKey(
            TraceFlowTask task,
            String traceCode,
            ActionType actionType,
            String idempotencyKey
    ) {
        // Validate a client-provided key for API contract compatibility, but use
        // a stable task/action/code key so continuous scans of the same code are
        // idempotent even when the scanner app generates a fresh request key.
        TraceLocationFieldConstraints.normalizeIdempotencyKey("idempotencyKey", idempotencyKey);
        UUID uuid = UUID.nameUUIDFromBytes((task.getId() + ":" + actionType.getCode() + ":" + traceCode)
                .getBytes(StandardCharsets.UTF_8));
        return "FLOW-SCAN-" + uuid.toString().toUpperCase();
    }

    private record TaskScanPlan(
            ActionType actionType,
            String fromNode,
            String toNode,
            TraceNode operationNode,
            boolean countsTowardsActualQuantity
    ) {
    }

    private record TaskScanTargets(
            String parentCode,
            boolean batchScan,
            List<String> childTraceCodes
    ) {
    }

    private record BatchChildScanPlan(
            String traceCode,
            TaskScanPlan scanPlan
    ) {
    }

    private TraceFlowTaskStatus parseStatus(TraceFlowTask task) {
        try {
            return TraceFlowTaskStatus.fromString(task.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "流转任务状态非法: taskId=" + task.getId() + ", status=" + task.getStatus());
        }
    }

    private TraceFlowTaskType parseTaskType(TraceFlowTask task) {
        try {
            return TraceFlowTaskType.fromString(task.getTaskType());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "流转任务类型非法: taskId=" + task.getId() + ", taskType=" + task.getTaskType());
        }
    }

    private List<TraceFlowTaskResponse> toResponses(List<TraceFlowTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        List<Long> nodeIds = tasks.stream()
                .flatMap(task -> Stream.of(task.getSourceNodeId(), task.getTargetNodeId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, TraceNode> nodesById = nodeIds.isEmpty()
                ? Map.of()
                : traceNodeMapper.selectBatchIds(nodeIds).stream()
                        .collect(Collectors.toMap(TraceNode::getId, Function.identity()));
        return tasks.stream()
                .map(task -> toResponse(task, nodesById.get(task.getSourceNodeId()), nodesById.get(task.getTargetNodeId())))
                .toList();
    }

    private TraceFlowTaskResponse toResponse(TraceFlowTask task) {
        TraceNode sourceNode = task.getSourceNodeId() == null ? null : traceNodeMapper.selectById(task.getSourceNodeId());
        TraceNode targetNode = task.getTargetNodeId() == null ? null : traceNodeMapper.selectById(task.getTargetNodeId());
        return toResponse(task, sourceNode, targetNode);
    }

    private TraceFlowTaskResponse toResponse(TraceFlowTask task, TraceNode sourceNode, TraceNode targetNode) {
        TraceFlowTaskType taskType = parseTaskType(task);
        TraceFlowTaskStatus status = parseStatus(task);
        TraceFlowTaskDiscrepancyType discrepancyType = parseDiscrepancyType(task);
        return TraceFlowTaskResponse.builder()
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .taskType(taskType)
                .taskTypeLabel(taskType.getLabel())
                .status(status)
                .statusLabel(status.getLabel())
                .sourceNodeId(task.getSourceNodeId())
                .sourceNodeCode(sourceNode == null ? null : sourceNode.getNodeCode())
                .sourceNodeName(sourceNode == null ? null : sourceNode.getNodeName())
                .targetNodeId(task.getTargetNodeId())
                .targetNodeCode(targetNode == null ? null : targetNode.getNodeCode())
                .targetNodeName(targetNode == null ? null : targetNode.getNodeName())
                .expectedQuantity(task.getExpectedQuantity())
                .actualQuantity(task.getActualQuantity())
                .remainingQuantity(calculateRemainingQuantity(task))
                .discrepancyType(discrepancyType)
                .discrepancyTypeLabel(discrepancyType.getLabel())
                .discrepancyQuantity(task.getDiscrepancyQuantity())
                .discrepancyReason(task.getDiscrepancyReason())
                .createBy(task.getCreateBy())
                .createByUsername(task.getCreateByUsername())
                .remark(task.getRemark())
                .completeTime(task.getCompleteTime())
                .cancelTime(task.getCancelTime())
                .discrepancyTime(task.getDiscrepancyTime())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }

    private TraceFlowTaskDiscrepancyType parseDiscrepancyType(TraceFlowTask task) {
        if (!StringUtils.hasText(task.getDiscrepancyType())) {
            return TraceFlowTaskDiscrepancyType.NONE;
        }
        try {
            return TraceFlowTaskDiscrepancyType.fromString(task.getDiscrepancyType());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "流转任务差异类型非法: taskId=" + task.getId()
                            + ", discrepancyType=" + task.getDiscrepancyType());
        }
    }

    private Integer calculateRemainingQuantity(TraceFlowTask task) {
        if (task.getExpectedQuantity() == null || task.getActualQuantity() == null) {
            return null;
        }
        return Math.max(task.getExpectedQuantity() - task.getActualQuantity(), 0);
    }

    private LocalDateTime nowSeconds() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
