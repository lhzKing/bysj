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
 * 扫码后计算“当前用户还能做哪些动作”。
 *
 * <p>这不是单纯根据状态枚举返回按钮，而是按多层条件逐步过滤：
 * 码状态是否允许流转 → 生命周期状态机允许哪些动作 → 角色是否有动作权限 →
 * 用户是否绑定当前作业节点。最终结果直接影响前端扫码工作台展示哪些按钮。</p>
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
        if (codeEligibility.blocked() && currentStatus != TraceStatus.EXCEPTION) {
            // 单品码被作废、未激活等情况下，即使生命周期状态允许，也不能继续做普通流转。
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
        /*
         * 可执行动作的三层过滤：
         * 1. 状态机：当前状态理论上能发生哪些动作；
         * 2. 角色动作授权：当前角色能不能执行这些动作；
         * 3. 用户节点绑定：当前用户是否有这个现场节点的作业资格。
         */
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
        TraceStatus nextStatus = actionType == ActionType.EXCEPTION_CLOSE
                ? TraceStatus.EXCEPTION
                : traceTransitionPolicy.resolveNextStatus(currentStatus, actionType, null);
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
        /*
         * 入库动作的特殊判断：
         * 运输/流转中的 currentNode 通常表示“目标接收节点”，此时只要用户绑定了该节点，
         * 就允许做确认接收/入库。
         */
        if (actionType == ActionType.INBOUND
                && currentStatus == TraceStatus.IN_TRANSIT
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
        // 推荐动作优先选择正常业务流转动作，把异常/纠错类动作放在兜底位置。
        return availableActions.stream()
                .map(TraceAvailableActionsResponse.AvailableAction::getActionType)
                .filter(actionType -> actionType != ActionType.EXCEPTION
                        && actionType != ActionType.EXCEPTION_OPEN
                        && actionType != ActionType.EXCEPTION_CLOSE
                        && actionType != ActionType.CORRECTION)
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
        // 给前端和答辩演示一个可读原因，避免页面只显示“没有按钮”。
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
            case PACK -> "确认装箱";
            case UNPACK -> "确认拆箱";
            case PALLETIZE -> "确认托盘绑定";
            case UNPALLETIZE -> "确认托盘解绑";
            case INBOUND -> currentStatus == TraceStatus.IN_TRANSIT
                    ? "确认接收/入库"
                    : "确认入库";
            case OUTBOUND -> "确认出库";
            case TRANSFER -> "记录中转/流转";
            case DELIVER -> "最终签收/交付";
            case EXCEPTION, EXCEPTION_OPEN -> "上报异常并冻结";
            case EXCEPTION_CLOSE -> "解除异常冻结";
            case CORRECTION -> "提交审计纠错";
            case INIT -> "初始化";
        };
    }

    private boolean requiresRemark(ActionType actionType) {
        return actionType == ActionType.EXCEPTION
                || actionType == ActionType.EXCEPTION_OPEN
                || actionType == ActionType.EXCEPTION_CLOSE
                || actionType == ActionType.CORRECTION;
    }
}
