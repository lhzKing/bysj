package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAvailableActionsResponse;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.TraceUserNodeBindingService;
import com.example.trace.service.policy.TraceActionPermissionPolicy;
import com.example.trace.service.policy.TraceTransitionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Computes executable trace actions after a code is scanned.
 *
 * <p>B03 deliberately stays within the current minimal business loop: state and
 * role permissions are enforced now; structured node capability and flow-task
 * matching will be plugged in by B14-B20.</p>
 */
@Service
public class TraceAvailableActionService {

    private final TraceSnapshotMapper traceSnapshotMapper;
    private final TraceTransitionPolicy traceTransitionPolicy;
    private final TraceActionPermissionPolicy traceActionPermissionPolicy;
    private final TraceCodeStatusService traceCodeStatusService;
    private final TraceUserNodeBindingService traceUserNodeBindingService;

    public TraceAvailableActionService(
            TraceSnapshotMapper traceSnapshotMapper,
            TraceTransitionPolicy traceTransitionPolicy,
            TraceActionPermissionPolicy traceActionPermissionPolicy,
            TraceCodeStatusService traceCodeStatusService
    ) {
        this(
                traceSnapshotMapper,
                traceTransitionPolicy,
                traceActionPermissionPolicy,
                traceCodeStatusService,
                null
        );
    }

    @Autowired
    public TraceAvailableActionService(
            TraceSnapshotMapper traceSnapshotMapper,
            TraceTransitionPolicy traceTransitionPolicy,
            TraceActionPermissionPolicy traceActionPermissionPolicy,
            TraceCodeStatusService traceCodeStatusService,
            TraceUserNodeBindingService traceUserNodeBindingService
    ) {
        this.traceSnapshotMapper = traceSnapshotMapper;
        this.traceTransitionPolicy = traceTransitionPolicy;
        this.traceActionPermissionPolicy = traceActionPermissionPolicy;
        this.traceCodeStatusService = traceCodeStatusService;
        this.traceUserNodeBindingService = traceUserNodeBindingService;
    }

    public TraceAvailableActionsResponse availableActions(String traceCode, Long roleId) {
        return availableActions(traceCode, roleId, null);
    }

    public TraceAvailableActionsResponse availableActions(String traceCode, Long roleId, Long userId) {
        TraceSnapshot snapshot = traceSnapshotMapper.selectById(traceCode);
        if (snapshot == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND, "未知溯源码: " + traceCode);
        }

        TraceStatus currentStatus = TraceStatus.fromStringOrDefault(
                snapshot.getCurrentStatus(),
                TraceStatus.INIT
        );
        TraceCodeStatusService.MovementEligibility codeEligibility =
                traceCodeStatusService.movementEligibility(traceCode);
        if (codeEligibility.blocked()) {
            return TraceAvailableActionsResponse.builder()
                    .traceCode(snapshot.getTraceCode())
                    .currentStatus(currentStatus)
                    .currentStatusLabel(currentStatus.getName())
                    .currentNode(snapshot.getCurrentNode())
                    .recommendedAction(null)
                    .availableActions(List.of())
                    .noActionReason(codeEligibility.reason())
                    .build();
        }
        List<ActionType> stateAllowedActions = traceTransitionPolicy.allowedActions(currentStatus);
        List<ActionType> roleExecutableActions = traceActionPermissionPolicy.filterExecutable(roleId, stateAllowedActions);
        List<ActionType> executableActions = filterByUserNode(
                userId,
                currentStatus,
                snapshot.getCurrentNode(),
                roleExecutableActions
        );
        List<TraceAvailableActionsResponse.AvailableAction> availableActions = executableActions.stream()
                .map(actionType -> toAvailableAction(currentStatus, actionType))
                .toList();

        ActionType recommendedAction = chooseRecommendedAction(availableActions);
        String noActionReason = availableActions.isEmpty()
                ? buildNoActionReason(roleId, userId, currentStatus, stateAllowedActions, roleExecutableActions)
                : null;

        return TraceAvailableActionsResponse.builder()
                .traceCode(snapshot.getTraceCode())
                .currentStatus(currentStatus)
                .currentStatusLabel(currentStatus.getName())
                .currentNode(snapshot.getCurrentNode())
                .recommendedAction(recommendedAction)
                .availableActions(availableActions)
                .noActionReason(noActionReason)
                .build();
    }

    private TraceAvailableActionsResponse.AvailableAction toAvailableAction(
            TraceStatus currentStatus,
            ActionType actionType
    ) {
        TraceStatus nextStatus = traceTransitionPolicy.resolveNextStatus(currentStatus, actionType, null);
        return TraceAvailableActionsResponse.AvailableAction.builder()
                .actionType(actionType)
                .label(labelOf(currentStatus, actionType))
                .requiresRemark(requiresRemark(actionType))
                .nextStatus(nextStatus)
                .nextStatusLabel(nextStatus.getName())
                .permissionHint(traceActionPermissionPolicy.permissionHint(actionType))
                .build();
    }


    private List<ActionType> filterByUserNode(
            Long userId,
            TraceStatus currentStatus,
            String currentNode,
            List<ActionType> executableActions
    ) {
        if (traceUserNodeBindingService == null || userId == null || executableActions.isEmpty()) {
            return executableActions;
        }
        return executableActions.stream()
                .filter(actionType -> canExecuteActionAtNode(userId, currentStatus, currentNode, actionType))
                .toList();
    }

    private boolean canExecuteActionAtNode(
            Long userId,
            TraceStatus currentStatus,
            String currentNode,
            ActionType actionType
    ) {
        if (actionType == ActionType.INBOUND
                && (currentStatus == TraceStatus.IN_TRANSIT || currentStatus == TraceStatus.TRANSFERRED)
                && StringUtils.hasText(currentNode)) {
            return userHasBoundNode(userId, currentNode);
        }
        return traceUserNodeBindingService.canExecuteActionAtCurrentNode(userId, actionType, currentNode);
    }

    private boolean userHasBoundNode(Long userId, String nodeNameOrCode) {
        return traceUserNodeBindingService.listEnabledOperationNodes(userId).stream()
                .anyMatch(node -> matchesNode(node, nodeNameOrCode));
    }

    private boolean matchesNode(TraceNode node, String nodeNameOrCode) {
        if (node == null || !StringUtils.hasText(nodeNameOrCode)) {
            return false;
        }
        String normalized = nodeNameOrCode.trim();
        if (node.getNodeName() != null && node.getNodeName().equals(normalized)) {
            return true;
        }
        return node.getNodeCode() != null && node.getNodeCode().equalsIgnoreCase(normalized);
    }

    private ActionType chooseRecommendedAction(
            List<TraceAvailableActionsResponse.AvailableAction> availableActions
    ) {
        return availableActions.stream()
                .map(TraceAvailableActionsResponse.AvailableAction::getActionType)
                .filter(actionType -> actionType != ActionType.EXCEPTION && actionType != ActionType.CORRECTION)
                .findFirst()
                .or(() -> availableActions.stream()
                        .map(TraceAvailableActionsResponse.AvailableAction::getActionType)
                        .findFirst())
                .orElse(null);
    }

    private String buildNoActionReason(
            Long roleId,
            Long userId,
            TraceStatus currentStatus,
            List<ActionType> stateAllowedActions,
            List<ActionType> roleExecutableActions
    ) {
        if (roleId == null) {
            return "当前请求缺少角色上下文，无法判断可执行动作";
        }
        if (stateAllowedActions == null || stateAllowedActions.isEmpty()) {
            return "当前状态 " + currentStatus.getCode() + " 无常规可执行动作；如需纠错请进入审计纠错流程";
        }
        if (userId != null && roleExecutableActions != null && !roleExecutableActions.isEmpty()) {
            return "当前用户未绑定当前可操作节点，无法执行节点扫码动作";
        }
        return "当前角色没有该状态下的扫码动作权限；状态允许动作: "
                + stateAllowedActions.stream().map(ActionType::getCode).toList();
    }

    private String labelOf(TraceStatus currentStatus, ActionType actionType) {
        return switch (actionType) {
            case PRINT_CODE -> "打印标签";
            case REPRINT_CODE -> "重打标签";
            case ACTIVATE_CODE -> "扫码激活";
            case VOID_CODE -> "作废标签";
            case INBOUND -> currentStatus == TraceStatus.IN_TRANSIT || currentStatus == TraceStatus.TRANSFERRED
                    ? "确认接收/入库"
                    : "确认入库";
            case OUTBOUND -> "确认出库";
            case TRANSFER -> "确认流转/交接";
            case EXCEPTION -> "上报异常";
            case CORRECTION -> "提交审计纠错";
            case INIT -> "初始化";
        };
    }

    private boolean requiresRemark(ActionType actionType) {
        return actionType == ActionType.EXCEPTION || actionType == ActionType.CORRECTION;
    }
}
