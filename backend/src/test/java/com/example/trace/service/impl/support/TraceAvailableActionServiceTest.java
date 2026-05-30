package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAvailableActionsResponse;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceCodeStatus;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.security.PermissionService;
import com.example.trace.service.TraceUserNodeBindingService;
import com.example.trace.service.policy.TraceActionPermissionPolicy;
import com.example.trace.service.policy.TraceTransitionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceAvailableActionServiceTest {

    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private TraceCodeStatusService traceCodeStatusService;
    @Mock
    private TraceUserNodeBindingService traceUserNodeBindingService;

    private TraceAvailableActionService service;

    @BeforeEach
    void setUp() {
        service = new TraceAvailableActionService(
                traceSnapshotMapper,
                new TraceTransitionPolicy(),
                new TraceActionPermissionPolicy(permissionService),
                traceCodeStatusService
        );
    }

    @Test
    void availableActions_shouldReturnInboundForWarehouseAtInitStatus() {
        when(traceSnapshotMapper.selectById("TRACE-INIT"))
                .thenReturn(snapshot("TRACE-INIT", TraceStatus.INIT, "factory-A"));
        when(traceCodeStatusService.movementEligibility("TRACE-INIT"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(null));
        when(permissionService.getPermissionCodes(4L))
                .thenReturn(Set.of("trace:view", "trace:inbound"));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-INIT", 4L);

        assertThat(response.getTraceCode()).isEqualTo("TRACE-INIT");
        assertThat(response.getCurrentStatus()).isEqualTo(TraceStatus.INIT);
        assertThat(response.getCurrentNode()).isEqualTo("factory-A");
        assertThat(response.getRecommendedAction()).isEqualTo(ActionType.INBOUND);
        assertThat(response.getNoActionReason()).isNull();
        assertThat(response.getAvailableActions())
                .extracting(TraceAvailableActionsResponse.AvailableAction::getActionType)
                .containsExactly(ActionType.INBOUND);
        TraceAvailableActionsResponse.AvailableAction action = response.getAvailableActions().get(0);
        assertThat(action.isRequiresRemark()).isFalse();
        assertThat(action.getNextStatus()).isEqualTo(TraceStatus.IN_STOCK);
        assertThat(action.getPermissionHint()).isEqualTo("trace:inbound or trace:scan");
    }

    @Test
    void availableActions_shouldReturnTransferAndDeliverForLogisticsAtInTransitStatus() {
        when(traceSnapshotMapper.selectById("TRACE-MOVE"))
                .thenReturn(snapshot("TRACE-MOVE", TraceStatus.IN_TRANSIT, "in-transit"));
        when(traceCodeStatusService.movementEligibility("TRACE-MOVE"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(TraceCodeStatus.IN_TRANSIT));
        when(permissionService.getPermissionCodes(5L))
                .thenReturn(Set.of("trace:view", "trace:transfer"));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-MOVE", 5L);

        assertThat(response.getRecommendedAction()).isEqualTo(ActionType.TRANSFER);
        assertThat(response.getAvailableActions())
                .extracting(TraceAvailableActionsResponse.AvailableAction::getActionType)
                .containsExactly(ActionType.TRANSFER, ActionType.DELIVER);
        assertThat(response.getAvailableActions())
                .filteredOn(action -> action.getActionType() == ActionType.TRANSFER)
                .singleElement()
                .extracting(TraceAvailableActionsResponse.AvailableAction::getNextStatus)
                .isEqualTo(TraceStatus.IN_TRANSIT);
        assertThat(response.getAvailableActions())
                .filteredOn(action -> action.getActionType() == ActionType.DELIVER)
                .singleElement()
                .extracting(TraceAvailableActionsResponse.AvailableAction::getNextStatus)
                .isEqualTo(TraceStatus.TRANSFERRED);
    }

    @Test
    void availableActions_shouldReturnReceiveInboundAfterOutboundMovedTraceToTargetNode() {
        when(traceSnapshotMapper.selectById("TRACE-OUTBOUND"))
                .thenReturn(snapshot("TRACE-OUTBOUND", TraceStatus.IN_TRANSIT, "上海仓库"));
        when(traceCodeStatusService.movementEligibility("TRACE-OUTBOUND"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(TraceCodeStatus.IN_TRANSIT));
        when(permissionService.getPermissionCodes(4L))
                .thenReturn(Set.of("trace:view", "trace:inbound"));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-OUTBOUND", 4L);

        assertThat(response.getRecommendedAction()).isEqualTo(ActionType.INBOUND);
        assertThat(response.getAvailableActions())
                .extracting(TraceAvailableActionsResponse.AvailableAction::getActionType)
                .containsExactly(ActionType.INBOUND);
        assertThat(response.getAvailableActions().get(0).getLabel()).isEqualTo("确认接收/入库");
    }

    @Test
    void availableActions_shouldHideInboundWhenTraceIsTransferredTerminal() {
        when(traceSnapshotMapper.selectById("TRACE-DONE"))
                .thenReturn(snapshot("TRACE-DONE", TraceStatus.TRANSFERRED, "客户签收点"));
        when(traceCodeStatusService.movementEligibility("TRACE-DONE"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.blocked(
                        TraceCodeStatus.TRANSFERRED,
                        "单品码状态为 TRANSFERRED，已完成交付，不能再次入库"
                ));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-DONE", 4L);

        assertThat(response.getRecommendedAction()).isNull();
        assertThat(response.getAvailableActions()).isEmpty();
        assertThat(response.getNoActionReason()).contains("TRANSFERRED").contains("不能再次入库");
    }

    @Test
    void availableActions_shouldFilterReceiveByTargetNodeBindingWhenTraceIsInTransit() {
        TraceAvailableActionService nodeAwareService = new TraceAvailableActionService(
                traceSnapshotMapper,
                new TraceTransitionPolicy(),
                new TraceActionPermissionPolicy(permissionService),
                traceCodeStatusService,
                traceUserNodeBindingService
        );
        when(traceSnapshotMapper.selectById("TRACE-RECEIVE"))
                .thenReturn(snapshot("TRACE-RECEIVE", TraceStatus.IN_TRANSIT, "上海仓库"));
        when(traceCodeStatusService.movementEligibility("TRACE-RECEIVE"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(TraceCodeStatus.IN_TRANSIT));
        when(permissionService.getPermissionCodes(4L))
                .thenReturn(Set.of("trace:view", "trace:inbound"));
        when(traceUserNodeBindingService.listEnabledOperationNodes(7L))
                .thenReturn(Set.of(node("WAREHOUSE-SH", "上海仓库")).stream().toList());

        TraceAvailableActionsResponse response = nodeAwareService.availableActions("TRACE-RECEIVE", 4L, 7L);

        assertThat(response.getRecommendedAction()).isEqualTo(ActionType.INBOUND);
        assertThat(response.getAvailableActions())
                .extracting(TraceAvailableActionsResponse.AvailableAction::getActionType)
                .containsExactly(ActionType.INBOUND);
    }

    @Test
    void availableActions_shouldHideReceiveWhenUserIsNotBoundToTransitTargetNode() {
        TraceAvailableActionService nodeAwareService = new TraceAvailableActionService(
                traceSnapshotMapper,
                new TraceTransitionPolicy(),
                new TraceActionPermissionPolicy(permissionService),
                traceCodeStatusService,
                traceUserNodeBindingService
        );
        when(traceSnapshotMapper.selectById("TRACE-RECEIVE"))
                .thenReturn(snapshot("TRACE-RECEIVE", TraceStatus.IN_TRANSIT, "上海仓库"));
        when(traceCodeStatusService.movementEligibility("TRACE-RECEIVE"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(TraceCodeStatus.IN_TRANSIT));
        when(permissionService.getPermissionCodes(4L))
                .thenReturn(Set.of("trace:view", "trace:inbound"));
        when(traceUserNodeBindingService.listEnabledOperationNodes(7L))
                .thenReturn(Set.of(node("WAREHOUSE-GZ", "广州仓库")).stream().toList());

        TraceAvailableActionsResponse response = nodeAwareService.availableActions("TRACE-RECEIVE", 4L, 7L);

        assertThat(response.getRecommendedAction()).isNull();
        assertThat(response.getAvailableActions()).isEmpty();
        assertThat(response.getNoActionReason()).contains("未绑定").contains("节点");
    }

    @Test
    void availableActions_shouldReturnNoActionReasonForProducerWithoutScanPermissions() {
        when(traceSnapshotMapper.selectById("TRACE-STOCK"))
                .thenReturn(snapshot("TRACE-STOCK", TraceStatus.IN_STOCK, "warehouse-A"));
        when(traceCodeStatusService.movementEligibility("TRACE-STOCK"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(TraceCodeStatus.IN_STOCK));
        when(permissionService.getPermissionCodes(3L))
                .thenReturn(Set.of("trace:create", "trace:view"));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-STOCK", 3L);

        assertThat(response.getRecommendedAction()).isNull();
        assertThat(response.getAvailableActions()).isEmpty();
        assertThat(response.getNoActionReason()).contains("OUTBOUND");
    }

    @Test
    void availableActions_shouldReturnNoActionReasonForQueryOnlyUser() {
        when(traceSnapshotMapper.selectById("TRACE-QUERY"))
                .thenReturn(snapshot("TRACE-QUERY", TraceStatus.INIT, "factory-A"));
        when(traceCodeStatusService.movementEligibility("TRACE-QUERY"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(null));
        when(permissionService.getPermissionCodes(6L))
                .thenReturn(Set.of("trace:view"));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-QUERY", 6L);

        assertThat(response.getAvailableActions()).isEmpty();
        assertThat(response.getNoActionReason()).isNotBlank();
    }

    @Test
    void availableActions_shouldExposeExceptionActionForSuperScanButNotRecommendItFirst() {
        when(traceSnapshotMapper.selectById("TRACE-SUPER"))
                .thenReturn(snapshot("TRACE-SUPER", TraceStatus.IN_STOCK, "warehouse-A"));
        when(traceCodeStatusService.movementEligibility("TRACE-SUPER"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(TraceCodeStatus.IN_STOCK));
        when(permissionService.getPermissionCodes(2L))
                .thenReturn(Set.of("trace:view", "trace:scan"));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-SUPER", 2L);

        assertThat(response.getRecommendedAction()).isEqualTo(ActionType.OUTBOUND);
        assertThat(response.getAvailableActions())
                .extracting(TraceAvailableActionsResponse.AvailableAction::getActionType)
                .containsExactly(ActionType.OUTBOUND, ActionType.EXCEPTION_OPEN);
        TraceAvailableActionsResponse.AvailableAction exceptionAction = response.getAvailableActions().get(1);
        assertThat(exceptionAction.isRequiresRemark()).isTrue();
        assertThat(exceptionAction.getNextStatus()).isEqualTo(TraceStatus.EXCEPTION);
    }

    @Test
    void availableActions_shouldExplainFrozenExceptionStatus() {
        when(traceSnapshotMapper.selectById("TRACE-ERROR"))
                .thenReturn(snapshot("TRACE-ERROR", TraceStatus.EXCEPTION, "exception-node"));
        when(traceCodeStatusService.movementEligibility("TRACE-ERROR"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(TraceCodeStatus.EXCEPTION));
        when(permissionService.getPermissionCodes(2L))
                .thenReturn(Set.of("trace:view"));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-ERROR", 2L);

        assertThat(response.getAvailableActions()).isEmpty();
        assertThat(response.getRecommendedAction()).isNull();
        assertThat(response.getNoActionReason()).contains("EXCEPTION");
    }

    @Test
    void availableActions_shouldExposeExceptionCloseForExceptionHandler() {
        when(traceSnapshotMapper.selectById("TRACE-ERROR"))
                .thenReturn(snapshot("TRACE-ERROR", TraceStatus.EXCEPTION, "exception-node"));
        when(traceCodeStatusService.movementEligibility("TRACE-ERROR"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.blocked(
                        TraceCodeStatus.EXCEPTION,
                        "异常冻结中"
                ));
        when(permissionService.getPermissionCodes(7L))
                .thenReturn(Set.of("trace:view", "trace:exception:handle"));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-ERROR", 7L);

        assertThat(response.getRecommendedAction()).isEqualTo(ActionType.EXCEPTION_CLOSE);
        assertThat(response.getAvailableActions())
                .extracting(TraceAvailableActionsResponse.AvailableAction::getActionType)
                .containsExactly(ActionType.EXCEPTION_CLOSE);
        assertThat(response.getAvailableActions().get(0).isRequiresRemark()).isTrue();
        assertThat(response.getAvailableActions().get(0).getLabel()).isEqualTo("解除异常冻结");
        assertThat(response.getAvailableActions().get(0).getNextStatus()).isEqualTo(TraceStatus.EXCEPTION);
        assertThat(response.getNoActionReason()).isNull();
    }

    @Test
    void availableActions_shouldExplainUnactivatedTraceCodeStatusBeforeRoleFiltering() {
        when(traceSnapshotMapper.selectById("TRACE-GENERATED"))
                .thenReturn(snapshot("TRACE-GENERATED", TraceStatus.INIT, "factory-A"));
        when(traceCodeStatusService.movementEligibility("TRACE-GENERATED"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.blocked(
                        TraceCodeStatus.GENERATED,
                        "code status GENERATED is not activated"
                ));

        TraceAvailableActionsResponse response = service.availableActions("TRACE-GENERATED", 4L);

        assertThat(response.getAvailableActions()).isEmpty();
        assertThat(response.getRecommendedAction()).isNull();
        assertThat(response.getNoActionReason()).contains("GENERATED").contains("not activated");
    }


    @Test
    void availableActions_shouldFilterNodeActionsByCurrentUserBindings() {
        TraceAvailableActionService nodeAwareService = new TraceAvailableActionService(
                traceSnapshotMapper,
                new TraceTransitionPolicy(),
                new TraceActionPermissionPolicy(permissionService),
                traceCodeStatusService,
                traceUserNodeBindingService
        );
        when(traceSnapshotMapper.selectById("TRACE-NODE"))
                .thenReturn(snapshot("TRACE-NODE", TraceStatus.IN_STOCK, "warehouse-A"));
        when(traceCodeStatusService.movementEligibility("TRACE-NODE"))
                .thenReturn(TraceCodeStatusService.MovementEligibility.allowed(TraceCodeStatus.IN_STOCK));
        when(permissionService.getPermissionCodes(4L))
                .thenReturn(Set.of("trace:view", "trace:outbound"));
        when(traceUserNodeBindingService.canExecuteActionAtCurrentNode(7L, ActionType.OUTBOUND, "warehouse-A"))
                .thenReturn(false);

        TraceAvailableActionsResponse response = nodeAwareService.availableActions("TRACE-NODE", 4L, 7L);

        assertThat(response.getAvailableActions()).isEmpty();
        assertThat(response.getRecommendedAction()).isNull();
        assertThat(response.getNoActionReason()).contains("未绑定").contains("节点");
    }

    @Test
    void availableActions_shouldThrowWhenTraceCodeDoesNotExist() {
        when(traceSnapshotMapper.selectById("TRACE-MISSING")).thenReturn(null);

        assertThatThrownBy(() -> service.availableActions("TRACE-MISSING", 4L))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(BizCode.TRACE_NOT_FOUND));
    }

    private static TraceSnapshot snapshot(String traceCode, TraceStatus status, String currentNode) {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode(traceCode);
        snapshot.setCurrentStatus(status.getCode());
        snapshot.setCurrentNode(currentNode);
        return snapshot;
    }

    private static TraceNode node(String nodeCode, String nodeName) {
        TraceNode node = new TraceNode();
        node.setNodeCode(nodeCode);
        node.setNodeName(nodeName);
        node.setEnabled(true);
        return node;
    }
}
