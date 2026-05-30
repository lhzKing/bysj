package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
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
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceFlowTaskScanMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.mapper.TraceFlowTaskMapper;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.service.impl.support.TraceScanRetryExecutor;
import com.example.trace.dto.ScanTraceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceFlowTaskServiceImplTest {

    @Mock
    private TraceAggregationMapper traceAggregationMapper;
    @Mock
    private TraceFlowTaskMapper traceFlowTaskMapper;
    @Mock
    private TraceFlowTaskScanMapper traceFlowTaskScanMapper;
    @Mock
    private TraceNodeMapper traceNodeMapper;
    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceScanRetryExecutor traceScanRetryExecutor;

    private TraceFlowTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraceFlowTaskServiceImpl(
                traceAggregationMapper,
                traceFlowTaskMapper,
                traceFlowTaskScanMapper,
                traceNodeMapper,
                traceSnapshotMapper,
                traceScanRetryExecutor
        );
    }

    @Test
    void createTask_shouldPersistWaybillLikeFlowTaskWithNodesAndOperator() {
        when(traceFlowTaskMapper.selectByTaskNo("SHIP-001")).thenReturn(null);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceFlowTaskMapper.insert(any(TraceFlowTask.class))).thenAnswer(invocation -> {
            TraceFlowTask task = invocation.getArgument(0);
            task.setId(99L);
            return 1;
        });

        TraceFlowTaskResponse response = service.createTask(createRequest(), 7L, "operator-a");

        ArgumentCaptor<TraceFlowTask> taskCaptor = ArgumentCaptor.forClass(TraceFlowTask.class);
        verify(traceFlowTaskMapper).insert(taskCaptor.capture());
        TraceFlowTask task = taskCaptor.getValue();
        assertThat(task.getTaskNo()).isEqualTo("SHIP-001");
        assertThat(task.getTaskType()).isEqualTo(TraceFlowTaskType.OUTBOUND.getCode());
        assertThat(task.getSourceNodeId()).isEqualTo(1L);
        assertThat(task.getTargetNodeId()).isEqualTo(2L);
        assertThat(task.getExpectedQuantity()).isEqualTo(100);
        assertThat(task.getActualQuantity()).isZero();
        assertThat(task.getStatus()).isEqualTo(TraceFlowTaskStatus.CREATED.getCode());
        assertThat(task.getDiscrepancyType()).isEqualTo(TraceFlowTaskDiscrepancyType.NONE.getCode());
        assertThat(task.getDiscrepancyQuantity()).isZero();
        assertThat(task.getCreateBy()).isEqualTo(7L);
        assertThat(task.getCreateByUsername()).isEqualTo("operator-a");
        assertThat(task.getRemark()).isEqualTo("计划发货");

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getTaskNo()).isEqualTo("SHIP-001");
        assertThat(response.getTaskType()).isEqualTo(TraceFlowTaskType.OUTBOUND);
        assertThat(response.getStatus()).isEqualTo(TraceFlowTaskStatus.CREATED);
        assertThat(response.getSourceNodeName()).isEqualTo("北京工厂");
        assertThat(response.getTargetNodeName()).isEqualTo("上海仓库");
    }

    @Test
    void createTask_shouldRejectDuplicateTaskNo() {
        when(traceFlowTaskMapper.selectByTaskNo("SHIP-001")).thenReturn(task(9L, TraceFlowTaskStatus.CREATED));

        assertThatThrownBy(() -> service.createTask(createRequest(), 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.CONFLICT));
    }

    @Test
    void createTask_shouldRejectDisabledNode() {
        when(traceFlowTaskMapper.selectByTaskNo("SHIP-001")).thenReturn(null);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", false));

        assertThatThrownBy(() -> service.createTask(createRequest(), 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.BAD_REQUEST));
    }

    @Test
    void cancelTask_shouldMoveNonTerminalTaskToCancelled() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));

        TraceFlowTaskResponse response = service.cancelTask(9L);

        assertThat(task.getStatus()).isEqualTo(TraceFlowTaskStatus.CANCELLED.getCode());
        assertThat(task.getCancelTime()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TraceFlowTaskStatus.CANCELLED);
        verify(traceFlowTaskMapper).updateById(task);
    }

    @Test
    void completeTask_shouldMoveTaskToCompletedWhenActualQuantityMatchesExpected() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(100);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        TraceFlowTaskCompleteRequest request = new TraceFlowTaskCompleteRequest();
        request.setRemark("数量一致，完成发货");

        TraceFlowTaskResponse response = service.completeTask(9L, request);

        assertThat(task.getStatus()).isEqualTo(TraceFlowTaskStatus.COMPLETED.getCode());
        assertThat(task.getActualQuantity()).isEqualTo(100);
        assertThat(task.getCompleteTime()).isNotNull();
        assertThat(task.getRemark()).isEqualTo("数量一致，完成发货");
        assertThat(task.getDiscrepancyType()).isEqualTo(TraceFlowTaskDiscrepancyType.NONE.getCode());
        assertThat(task.getDiscrepancyQuantity()).isZero();
        assertThat(task.getDiscrepancyReason()).isNull();
        assertThat(response.getStatus()).isEqualTo(TraceFlowTaskStatus.COMPLETED);
        assertThat(response.getDiscrepancyType()).isEqualTo(TraceFlowTaskDiscrepancyType.NONE);
        assertThat(response.getRemainingQuantity()).isZero();
        verify(traceFlowTaskMapper).updateById(task);
    }

    @Test
    void completeTask_shouldRejectQuantityMismatchWithoutDiscrepancyReason() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(98);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);

        assertThatThrownBy(() -> service.completeTask(9L, new TraceFlowTaskCompleteRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    assertThat(((BizException) error).getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(error.getMessage()).contains("必须填写差异原因");
                });
    }

    @Test
    void completeTask_shouldMarkShortageAsExceptionWhenReasonProvided() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(98);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        TraceFlowTaskCompleteRequest request = new TraceFlowTaskCompleteRequest();
        request.setDiscrepancyReason("装车短少 2 件，已线下登记");

        TraceFlowTaskResponse response = service.completeTask(9L, request);

        assertThat(task.getStatus()).isEqualTo(TraceFlowTaskStatus.EXCEPTION.getCode());
        assertThat(task.getActualQuantity()).isEqualTo(98);
        assertThat(task.getCompleteTime()).isNull();
        assertThat(task.getDiscrepancyType()).isEqualTo(TraceFlowTaskDiscrepancyType.SHORTAGE.getCode());
        assertThat(task.getDiscrepancyQuantity()).isEqualTo(2);
        assertThat(task.getDiscrepancyReason()).isEqualTo("装车短少 2 件，已线下登记");
        assertThat(task.getDiscrepancyTime()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TraceFlowTaskStatus.EXCEPTION);
        assertThat(response.getDiscrepancyType()).isEqualTo(TraceFlowTaskDiscrepancyType.SHORTAGE);
        assertThat(response.getDiscrepancyQuantity()).isEqualTo(2);
        verify(traceFlowTaskMapper).updateById(task);
    }

    @Test
    void completeTask_shouldMarkOverageAsExceptionWhenManualActualExceedsExpected() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(100);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        TraceFlowTaskCompleteRequest request = new TraceFlowTaskCompleteRequest();
        request.setActualQuantity(102);
        request.setDiscrepancyReason("现场多出 2 件待复核");

        TraceFlowTaskResponse response = service.completeTask(9L, request);

        assertThat(task.getStatus()).isEqualTo(TraceFlowTaskStatus.EXCEPTION.getCode());
        assertThat(task.getActualQuantity()).isEqualTo(102);
        assertThat(task.getDiscrepancyType()).isEqualTo(TraceFlowTaskDiscrepancyType.OVERAGE.getCode());
        assertThat(task.getDiscrepancyQuantity()).isEqualTo(2);
        assertThat(response.getDiscrepancyType()).isEqualTo(TraceFlowTaskDiscrepancyType.OVERAGE);
        assertThat(response.getRemainingQuantity()).isZero();
        verify(traceFlowTaskMapper).updateById(task);
    }

    @Test
    void completeTask_shouldRejectTerminalTask() {
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task(9L, TraceFlowTaskStatus.CANCELLED));

        assertThatThrownBy(() -> service.completeTask(9L, new TraceFlowTaskCompleteRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.BAD_REQUEST));
    }

    @Test
    void listTasks_shouldFilterAndEnrichNodes() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskMapper.selectList(any())).thenReturn(List.of(task));
        when(traceNodeMapper.selectBatchIds(List.of(1L, 2L)))
                .thenReturn(List.of(
                        node(1L, "FACTORY-BJ", "北京工厂", true),
                        node(2L, "WAREHOUSE-SH", "上海仓库", true)
                ));

        List<TraceFlowTaskResponse> responses = service.listTasks(TraceFlowTaskType.OUTBOUND, TraceFlowTaskStatus.CREATED);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getSourceNodeCode()).isEqualTo("FACTORY-BJ");
        assertThat(responses.get(0).getTargetNodeCode()).isEqualTo("WAREHOUSE-SH");
    }

    @Test
    void scanTask_shouldDriveOutboundEventFromTaskNodesAndIncrementActualQuantity() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "北京工厂"));
        when(traceScanRetryExecutor.executeAndReturnCreated(any(ScanTraceRequest.class), any()))
                .thenReturn(true);
        TraceFlowTaskScanRequest request = scanRequest(" TRACE-001 ");

        TraceFlowTaskResponse response = service.scanTask(9L, request, 7L, "operator-a");

        ArgumentCaptor<ScanTraceRequest> scanCaptor = ArgumentCaptor.forClass(ScanTraceRequest.class);
        verify(traceScanRetryExecutor).executeAndReturnCreated(scanCaptor.capture(), org.mockito.ArgumentMatchers.eq("operator-a"));
        ScanTraceRequest scanRequest = scanCaptor.getValue();
        assertThat(scanRequest.getTraceCode()).isEqualTo("TRACE-001");
        assertThat(scanRequest.getOperatorUserId()).isEqualTo(7L);
        assertThat(scanRequest.getActionType()).isEqualTo(ActionType.OUTBOUND);
        assertThat(scanRequest.getFromNode()).isEqualTo("北京工厂");
        assertThat(scanRequest.getToNode()).isEqualTo("上海仓库");
        assertThat(scanRequest.getProvince()).isEqualTo("上海市");
        assertThat(scanRequest.getCity()).isEqualTo("上海市");
        assertThat(scanRequest.getIdempotencyKey()).startsWith("FLOW-SCAN-");
        assertThat(scanRequest.getRemark()).isEqualTo("任务出库: SHIP-001");

        assertThat(task.getStatus()).isEqualTo(TraceFlowTaskStatus.PROCESSING.getCode());
        assertThat(task.getActualQuantity()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(TraceFlowTaskStatus.PROCESSING);
        assertThat(response.getActualQuantity()).isEqualTo(1);
        assertThat(response.getRemainingQuantity()).isEqualTo(99);
        assertThat(response.getLastScanTraceCode()).isEqualTo("TRACE-001");
        assertThat(response.getLastScanActionType()).isEqualTo(ActionType.OUTBOUND);
        assertThat(response.getLastScanCreated()).isTrue();
        assertThat(response.getDuplicateScan()).isFalse();
        assertThat(response.getScanMessage()).contains("扫码成功");
        ArgumentCaptor<TraceFlowTaskScan> taskScanCaptor = ArgumentCaptor.forClass(TraceFlowTaskScan.class);
        verify(traceFlowTaskScanMapper).insert(taskScanCaptor.capture());
        TraceFlowTaskScan taskScan = taskScanCaptor.getValue();
        assertThat(taskScan.getTaskId()).isEqualTo(9L);
        assertThat(taskScan.getTraceCode()).isEqualTo("TRACE-001");
        assertThat(taskScan.getActionType()).isEqualTo(ActionType.OUTBOUND.getCode());
        assertThat(taskScan.getCounted()).isTrue();
        assertThat(taskScan.getOperatorUserId()).isEqualTo(7L);
        assertThat(taskScan.getOperatorUsername()).isEqualTo("operator-a");
        assertThat(taskScan.getIdempotencyKey()).startsWith("FLOW-SCAN-");
        assertThat(taskScan.getDuplicateCount()).isZero();
        verify(traceFlowTaskMapper).updateById(task);
    }

    @Test
    void scanTask_shouldRejectTraceNotInSourceNodeStock() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "广州仓库"));

        assertThatThrownBy(() -> service.scanTask(9L, scanRequest("TRACE-001"), 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.BAD_REQUEST));
    }

    @Test
    void scanTask_shouldReturnCurrentTaskWithoutIncrementWhenScanIsIdempotentDuplicate() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(1);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "北京工厂"));
        when(traceScanRetryExecutor.executeAndReturnCreated(any(ScanTraceRequest.class), any()))
                .thenReturn(false);

        TraceFlowTaskResponse response = service.scanTask(9L, scanRequest("TRACE-001"), 7L, "operator-a");

        assertThat(task.getActualQuantity()).isEqualTo(1);
        assertThat(response.getActualQuantity()).isEqualTo(1);
        assertThat(response.getLastScanTraceCode()).isEqualTo("TRACE-001");
        assertThat(response.getLastScanActionType()).isEqualTo(ActionType.OUTBOUND);
        assertThat(response.getLastScanCreated()).isFalse();
        assertThat(response.getDuplicateScan()).isTrue();
        org.mockito.Mockito.verify(traceFlowTaskMapper, org.mockito.Mockito.never()).updateById(task);
        org.mockito.Mockito.verify(traceFlowTaskScanMapper, org.mockito.Mockito.never()).insert(any(TraceFlowTaskScan.class));
    }

    @Test
    void scanTask_shouldAccumulateContinuousScansAcrossDifferentCodes() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "北京工厂"));
        when(traceSnapshotMapper.selectById("TRACE-002"))
                .thenReturn(snapshot("TRACE-002", "IN_STOCK", "北京工厂"));
        when(traceScanRetryExecutor.executeAndReturnCreated(any(ScanTraceRequest.class), any()))
                .thenReturn(true);

        TraceFlowTaskResponse first = service.scanTask(9L, scanRequest("TRACE-001"), 7L, "operator-a");
        TraceFlowTaskResponse second = service.scanTask(9L, scanRequest("TRACE-002"), 7L, "operator-a");

        assertThat(first.getActualQuantity()).isEqualTo(1);
        assertThat(first.getRemainingQuantity()).isEqualTo(99);
        assertThat(second.getActualQuantity()).isEqualTo(2);
        assertThat(second.getRemainingQuantity()).isEqualTo(98);
        assertThat(second.getLastScanTraceCode()).isEqualTo("TRACE-002");
        assertThat(second.getDuplicateScan()).isFalse();
        org.mockito.Mockito.verify(traceFlowTaskScanMapper, org.mockito.Mockito.times(2)).insert(any(TraceFlowTaskScan.class));
        org.mockito.Mockito.verify(traceFlowTaskMapper, org.mockito.Mockito.times(2)).updateById(task);
    }

    @Test
    void scanTask_shouldExpandCartonParentAndCreateChildOutboundScans() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceAggregationMapper.selectActiveChildrenByParent("CARTON-001"))
                .thenReturn(List.of(
                        relation("CARTON-001", "TRACE-001"),
                        relation("CARTON-001", "TRACE-002")
                ));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "北京工厂"));
        when(traceSnapshotMapper.selectById("TRACE-002"))
                .thenReturn(snapshot("TRACE-002", "IN_STOCK", "北京工厂"));
        when(traceScanRetryExecutor.executeAndReturnCreated(any(ScanTraceRequest.class), any()))
                .thenReturn(true);

        TraceFlowTaskResponse response = service.scanTask(9L, scanRequest(" carton-001 "), 7L, "operator-a");

        ArgumentCaptor<ScanTraceRequest> scanCaptor = ArgumentCaptor.forClass(ScanTraceRequest.class);
        verify(traceScanRetryExecutor, org.mockito.Mockito.times(2))
                .executeAndReturnCreated(scanCaptor.capture(), org.mockito.ArgumentMatchers.eq("operator-a"));
        assertThat(scanCaptor.getAllValues())
                .extracting(ScanTraceRequest::getTraceCode)
                .containsExactly("TRACE-001", "TRACE-002");
        assertThat(scanCaptor.getAllValues())
                .allSatisfy(scan -> {
                    assertThat(scan.getActionType()).isEqualTo(ActionType.OUTBOUND);
                    assertThat(scan.getFromNode()).isEqualTo("北京工厂");
                    assertThat(scan.getToNode()).isEqualTo("上海仓库");
                });
        assertThat(task.getActualQuantity()).isEqualTo(2);
        assertThat(response.getActualQuantity()).isEqualTo(2);
        assertThat(response.getRemainingQuantity()).isEqualTo(98);
        assertThat(response.getLastScanTraceCode()).isEqualTo("CARTON-001");
        assertThat(response.getBatchScan()).isTrue();
        assertThat(response.getBatchParentCode()).isEqualTo("CARTON-001");
        assertThat(response.getBatchExpandedQuantity()).isEqualTo(2);
        assertThat(response.getBatchCreatedQuantity()).isEqualTo(2);
        assertThat(response.getBatchDuplicateQuantity()).isZero();
        assertThat(response.getScanMessage()).contains("展开 2 个单品码", "新增 2 个", "本次累计 2 件");
        org.mockito.Mockito.verify(traceFlowTaskScanMapper, org.mockito.Mockito.times(2))
                .insert(any(TraceFlowTaskScan.class));
        verify(traceFlowTaskMapper).updateById(task);
    }

    @Test
    void scanTask_shouldExpandPalletParentThroughCartonForReceiveTask() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        task.setTaskType(TraceFlowTaskType.RECEIVE.getCode());
        task.setExpectedQuantity(3);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceAggregationMapper.selectActiveChildrenByParent("PALLET-001"))
                .thenReturn(List.of(
                        relation("PALLET-001", "CARTON-001"),
                        relation("PALLET-001", "TRACE-003")
                ));
        when(traceAggregationMapper.selectActiveChildrenByParent("CARTON-001"))
                .thenReturn(List.of(
                        relation("CARTON-001", "TRACE-001"),
                        relation("CARTON-001", "TRACE-002")
                ));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_TRANSIT", "上海仓库"));
        when(traceSnapshotMapper.selectById("TRACE-002"))
                .thenReturn(snapshot("TRACE-002", "IN_TRANSIT", "上海仓库"));
        when(traceSnapshotMapper.selectById("TRACE-003"))
                .thenReturn(snapshot("TRACE-003", "IN_TRANSIT", "上海仓库"));
        when(traceScanRetryExecutor.executeAndReturnCreated(any(ScanTraceRequest.class), any()))
                .thenReturn(true);

        TraceFlowTaskResponse response = service.scanTask(9L, scanRequest("PALLET-001"), 7L, "receiver-a");

        ArgumentCaptor<ScanTraceRequest> scanCaptor = ArgumentCaptor.forClass(ScanTraceRequest.class);
        verify(traceScanRetryExecutor, org.mockito.Mockito.times(3))
                .executeAndReturnCreated(scanCaptor.capture(), org.mockito.ArgumentMatchers.eq("receiver-a"));
        assertThat(scanCaptor.getAllValues())
                .extracting(ScanTraceRequest::getTraceCode)
                .containsExactly("TRACE-001", "TRACE-002", "TRACE-003");
        assertThat(scanCaptor.getAllValues())
                .extracting(ScanTraceRequest::getActionType)
                .containsOnly(ActionType.INBOUND);
        assertThat(task.getActualQuantity()).isEqualTo(3);
        assertThat(response.getTaskType()).isEqualTo(TraceFlowTaskType.RECEIVE);
        assertThat(response.getBatchParentCode()).isEqualTo("PALLET-001");
        assertThat(response.getBatchExpandedQuantity()).isEqualTo(3);
        assertThat(response.getBatchCreatedQuantity()).isEqualTo(3);
    }

    @Test
    void scanTask_shouldRejectParentScanWhenExpandedQuantityExceedsRemainingCapacity() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setExpectedQuantity(2);
        task.setActualQuantity(1);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceAggregationMapper.selectActiveChildrenByParent("CARTON-001"))
                .thenReturn(List.of(
                        relation("CARTON-001", "TRACE-001"),
                        relation("CARTON-001", "TRACE-002")
                ));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "北京工厂"));
        when(traceSnapshotMapper.selectById("TRACE-002"))
                .thenReturn(snapshot("TRACE-002", "IN_STOCK", "北京工厂"));

        assertThatThrownBy(() -> service.scanTask(9L, scanRequest("CARTON-001"), 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    assertThat(((BizException) error).getCode()).isEqualTo(BizCode.CONFLICT);
                    assertThat(error.getMessage()).contains("超过任务剩余容量 1");
                });
        org.mockito.Mockito.verify(traceScanRetryExecutor, org.mockito.Mockito.never())
                .executeAndReturnCreated(any(), any());
        org.mockito.Mockito.verify(traceFlowTaskScanMapper, org.mockito.Mockito.never())
                .insert(any(TraceFlowTaskScan.class));
        org.mockito.Mockito.verify(traceFlowTaskMapper, org.mockito.Mockito.never()).updateById(task);
    }

    @Test
    void scanTask_shouldReportDuplicateOnlyParentBatchWithoutChangingTaskProgress() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(2);
        TraceFlowTaskScan firstScan = taskScan(77L, 9L, "TRACE-001", ActionType.OUTBOUND, true);
        TraceFlowTaskScan secondScan = taskScan(78L, 9L, "TRACE-002", ActionType.OUTBOUND, true);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceAggregationMapper.selectActiveChildrenByParent("CARTON-001"))
                .thenReturn(List.of(
                        relation("CARTON-001", "TRACE-001"),
                        relation("CARTON-001", "TRACE-002")
                ));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "北京工厂"));
        when(traceSnapshotMapper.selectById("TRACE-002"))
                .thenReturn(snapshot("TRACE-002", "IN_STOCK", "北京工厂"));
        when(traceFlowTaskScanMapper.selectByTaskTraceAction(9L, "TRACE-001", ActionType.OUTBOUND.getCode()))
                .thenReturn(firstScan);
        when(traceFlowTaskScanMapper.selectByTaskTraceAction(9L, "TRACE-002", ActionType.OUTBOUND.getCode()))
                .thenReturn(secondScan);

        TraceFlowTaskResponse response = service.scanTask(9L, scanRequest("CARTON-001"), 7L, "operator-a");

        assertThat(task.getActualQuantity()).isEqualTo(2);
        assertThat(response.getBatchScan()).isTrue();
        assertThat(response.getBatchParentCode()).isEqualTo("CARTON-001");
        assertThat(response.getBatchExpandedQuantity()).isEqualTo(2);
        assertThat(response.getBatchCreatedQuantity()).isZero();
        assertThat(response.getBatchDuplicateQuantity()).isEqualTo(2);
        assertThat(response.getDuplicateScan()).isTrue();
        assertThat(response.getScanMessage()).contains("新增 0 个", "重复 2 个", "不重复计数");
        verify(traceFlowTaskScanMapper).incrementDuplicateCount(77L);
        verify(traceFlowTaskScanMapper).incrementDuplicateCount(78L);
        org.mockito.Mockito.verify(traceScanRetryExecutor, org.mockito.Mockito.never())
                .executeAndReturnCreated(any(), any());
        org.mockito.Mockito.verify(traceFlowTaskMapper, org.mockito.Mockito.never()).updateById(task);
    }

    @Test
    void scanTask_shouldRejectWholeParentScanWhenAnyChildIsInvalid() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceAggregationMapper.selectActiveChildrenByParent("CARTON-001"))
                .thenReturn(List.of(
                        relation("CARTON-001", "TRACE-001"),
                        relation("CARTON-001", "TRACE-002")
                ));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "北京工厂"));
        when(traceSnapshotMapper.selectById("TRACE-002"))
                .thenReturn(snapshot("TRACE-002", "EXCEPTION", "北京工厂"));

        assertThatThrownBy(() -> service.scanTask(9L, scanRequest("CARTON-001"), 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    assertThat(((BizException) error).getCode()).isEqualTo(BizCode.INVALID_ACTION_TYPE);
                    assertThat(error.getMessage()).contains("父码 CARTON-001 内存在不可流转子码");
                    assertThat(error.getMessage()).contains("TRACE-002");
                });
        org.mockito.Mockito.verify(traceScanRetryExecutor, org.mockito.Mockito.never())
                .executeAndReturnCreated(any(), any());
        org.mockito.Mockito.verify(traceFlowTaskScanMapper, org.mockito.Mockito.never())
                .insert(any(TraceFlowTaskScan.class));
        org.mockito.Mockito.verify(traceFlowTaskMapper, org.mockito.Mockito.never()).updateById(task);
    }

    @Test
    void scanTask_shouldReturnAlreadyScannedFeedbackForRepeatedCodeWithoutCountingAgain() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(1);
        TraceFlowTaskScan existingScan = taskScan(77L, 9L, "TRACE-001", ActionType.OUTBOUND, true);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "北京工厂"));
        when(traceFlowTaskScanMapper.selectLatestByTaskTrace(9L, "TRACE-001")).thenReturn(existingScan);
        when(traceFlowTaskScanMapper.selectByTaskTraceAction(9L, "TRACE-001", ActionType.OUTBOUND.getCode()))
                .thenReturn(existingScan);

        TraceFlowTaskResponse response = service.scanTask(9L, scanRequest("TRACE-001"), 7L, "operator-a");

        assertThat(task.getActualQuantity()).isEqualTo(1);
        assertThat(response.getActualQuantity()).isEqualTo(1);
        assertThat(response.getRemainingQuantity()).isEqualTo(99);
        assertThat(response.getLastScanTraceCode()).isEqualTo("TRACE-001");
        assertThat(response.getLastScanActionType()).isEqualTo(ActionType.OUTBOUND);
        assertThat(response.getLastScanCreated()).isFalse();
        assertThat(response.getDuplicateScan()).isTrue();
        assertThat(response.getScanMessage()).contains("已在当前任务内扫码");
        verify(traceFlowTaskScanMapper).incrementDuplicateCount(77L);
        org.mockito.Mockito.verify(traceScanRetryExecutor, org.mockito.Mockito.never())
                .executeAndReturnCreated(any(), any());
        org.mockito.Mockito.verify(traceFlowTaskMapper, org.mockito.Mockito.never()).updateById(task);
    }

    @Test
    void scanTask_shouldReturnDuplicateFeedbackWhenAlreadyReceivedCodeNoLongerMatchesReceiveState() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setTaskType(TraceFlowTaskType.RECEIVE.getCode());
        task.setActualQuantity(1);
        TraceFlowTaskScan existingScan = taskScan(77L, 9L, "TRACE-001", ActionType.INBOUND, true);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_STOCK", "上海仓库"));
        when(traceFlowTaskScanMapper.selectLatestByTaskTrace(9L, "TRACE-001")).thenReturn(existingScan);

        TraceFlowTaskResponse response = service.scanTask(9L, scanRequest("TRACE-001"), 7L, "receiver-a");

        assertThat(response.getTaskType()).isEqualTo(TraceFlowTaskType.RECEIVE);
        assertThat(response.getActualQuantity()).isEqualTo(1);
        assertThat(response.getLastScanActionType()).isEqualTo(ActionType.INBOUND);
        assertThat(response.getLastScanCreated()).isFalse();
        assertThat(response.getDuplicateScan()).isTrue();
        verify(traceFlowTaskScanMapper).incrementDuplicateCount(77L);
        org.mockito.Mockito.verify(traceScanRetryExecutor, org.mockito.Mockito.never())
                .executeAndReturnCreated(any(), any());
    }

    @Test
    void scanTask_shouldReceiveInTransitTraceAtTargetNodeOnOutboundTaskWithoutDoubleCounting() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(1);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_TRANSIT", "上海仓库"));
        when(traceScanRetryExecutor.executeAndReturnCreated(any(ScanTraceRequest.class), any()))
                .thenReturn(true);

        TraceFlowTaskResponse response = service.scanTask(9L, scanRequest("TRACE-001"), 7L, "receiver-a");

        ArgumentCaptor<ScanTraceRequest> scanCaptor = ArgumentCaptor.forClass(ScanTraceRequest.class);
        verify(traceScanRetryExecutor).executeAndReturnCreated(scanCaptor.capture(), org.mockito.ArgumentMatchers.eq("receiver-a"));
        ScanTraceRequest scanRequest = scanCaptor.getValue();
        assertThat(scanRequest.getActionType()).isEqualTo(ActionType.INBOUND);
        assertThat(scanRequest.getFromNode()).isEqualTo("上海仓库");
        assertThat(scanRequest.getToNode()).isEqualTo("上海仓库");
        assertThat(scanRequest.getRemark()).isEqualTo("任务接收: SHIP-001");

        assertThat(task.getActualQuantity()).isEqualTo(1);
        assertThat(response.getActualQuantity()).isEqualTo(1);
        verify(traceFlowTaskMapper).updateById(task);
    }

    @Test
    void scanTask_shouldCountReceiveTaskWhenTargetNodeReceivesInTransitTrace() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        task.setTaskType(TraceFlowTaskType.RECEIVE.getCode());
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_TRANSIT", "上海仓库"));
        when(traceScanRetryExecutor.executeAndReturnCreated(any(ScanTraceRequest.class), any()))
                .thenReturn(true);

        TraceFlowTaskResponse response = service.scanTask(9L, scanRequest("TRACE-001"), 7L, "receiver-a");

        ArgumentCaptor<ScanTraceRequest> scanCaptor = ArgumentCaptor.forClass(ScanTraceRequest.class);
        verify(traceScanRetryExecutor).executeAndReturnCreated(scanCaptor.capture(), org.mockito.ArgumentMatchers.eq("receiver-a"));
        assertThat(scanCaptor.getValue().getActionType()).isEqualTo(ActionType.INBOUND);
        assertThat(task.getStatus()).isEqualTo(TraceFlowTaskStatus.PROCESSING.getCode());
        assertThat(task.getActualQuantity()).isEqualTo(1);
        assertThat(response.getTaskType()).isEqualTo(TraceFlowTaskType.RECEIVE);
        assertThat(response.getActualQuantity()).isEqualTo(1);
    }

    @Test
    void scanTask_shouldRejectReceiveWhenTraceIsAlreadyTransferredTerminal() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.CREATED);
        task.setTaskType(TraceFlowTaskType.RECEIVE.getCode());
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-DONE"))
                .thenReturn(snapshot("TRACE-DONE", "TRANSFERRED", "上海仓库"));

        assertThatThrownBy(() -> service.scanTask(9L, scanRequest("TRACE-DONE"), 7L, "receiver-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    assertThat(((BizException) error).getCode()).isEqualTo(BizCode.INVALID_ACTION_TYPE);
                    assertThat(error.getMessage()).contains("currentStatus=TRANSFERRED");
                });
    }

    @Test
    void scanTask_shouldRejectReceiveWhenTraceIsNotAtTargetNode() {
        TraceFlowTask task = task(9L, TraceFlowTaskStatus.PROCESSING);
        task.setActualQuantity(1);
        when(traceFlowTaskMapper.selectById(9L)).thenReturn(task);
        when(traceNodeMapper.selectById(1L)).thenReturn(node(1L, "FACTORY-BJ", "北京工厂", true));
        when(traceNodeMapper.selectById(2L)).thenReturn(node(2L, "WAREHOUSE-SH", "上海仓库", true));
        when(traceSnapshotMapper.selectById("TRACE-001"))
                .thenReturn(snapshot("TRACE-001", "IN_TRANSIT", "广州仓库"));

        assertThatThrownBy(() -> service.scanTask(9L, scanRequest("TRACE-001"), 7L, "receiver-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    assertThat(((BizException) error).getCode()).isEqualTo(BizCode.BAD_REQUEST);
                    assertThat(error.getMessage()).contains("接收任务目标节点");
                });
    }

    private static TraceFlowTaskCreateRequest createRequest() {
        TraceFlowTaskCreateRequest request = new TraceFlowTaskCreateRequest();
        request.setTaskNo(" ship-001 ");
        request.setTaskType(TraceFlowTaskType.OUTBOUND);
        request.setSourceNodeId(1L);
        request.setTargetNodeId(2L);
        request.setExpectedQuantity(100);
        request.setRemark("  计划发货  ");
        return request;
    }

    private static TraceFlowTaskScanRequest scanRequest(String traceCode) {
        TraceFlowTaskScanRequest request = new TraceFlowTaskScanRequest();
        request.setTraceCode(traceCode);
        return request;
    }

    private static TraceFlowTask task(Long id, TraceFlowTaskStatus status) {
        TraceFlowTask task = new TraceFlowTask();
        task.setId(id);
        task.setTaskNo("SHIP-001");
        task.setTaskType(TraceFlowTaskType.OUTBOUND.getCode());
        task.setSourceNodeId(1L);
        task.setTargetNodeId(2L);
        task.setExpectedQuantity(100);
        task.setActualQuantity(0);
        task.setStatus(status.getCode());
        task.setCreateBy(7L);
        task.setCreateByUsername("operator-a");
        return task;
    }

    private static TraceFlowTaskScan taskScan(
            Long id,
            Long taskId,
            String traceCode,
            ActionType actionType,
            boolean counted
    ) {
        TraceFlowTaskScan scan = new TraceFlowTaskScan();
        scan.setId(id);
        scan.setTaskId(taskId);
        scan.setTraceCode(traceCode);
        scan.setActionType(actionType.getCode());
        scan.setCounted(counted);
        scan.setDuplicateCount(0);
        return scan;
    }

    private static TraceAggregation relation(String parentCode, String childCode) {
        TraceAggregation relation = new TraceAggregation();
        relation.setParentCode(parentCode);
        relation.setChildCode(childCode);
        relation.setActive(true);
        return relation;
    }

    private static TraceNode node(Long id, String code, String name, boolean enabled) {
        TraceNode node = new TraceNode();
        node.setId(id);
        node.setNodeCode(code);
        node.setNodeName(name);
        node.setNodeType("WAREHOUSE");
        node.setOrgId(10L);
        node.setProvince("上海市");
        node.setCity("上海市");
        node.setEnabled(enabled);
        return node;
    }

    private static TraceSnapshot snapshot(String traceCode, String status, String currentNode) {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode(traceCode);
        snapshot.setCurrentStatus(status);
        snapshot.setCurrentNode(currentNode);
        return snapshot;
    }
}
