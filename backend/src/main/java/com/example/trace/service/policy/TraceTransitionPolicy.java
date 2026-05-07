package com.example.trace.service.policy;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized trace lifecycle transition policy.
 *
 * <p>The previous scan write path used {@link TraceStatus#deriveFromAction(TraceStatus, ActionType)}
 * as a direct mapping from event to next state. That was too permissive because it did not consider
 * the current state. This policy is the single place that defines the legal
 * {@code currentStatus + actionType -> nextStatus} triples for the current minimal business loop.</p>
 */
@Component
public class TraceTransitionPolicy {

    private static final Map<TraceStatus, Map<ActionType, TraceStatus>> TRANSITIONS = createTransitions();

    /**
     * Resolve and validate the next status for a scan event.
     *
     * @param currentStatus current snapshot status
     * @param actionType requested lifecycle event
     * @param correctionOf correction target id, only valid for {@link ActionType#CORRECTION}
     * @return next status; for {@code CORRECTION}, returns the current status unchanged
     */
    public TraceStatus resolveNextStatus(TraceStatus currentStatus, ActionType actionType, Long correctionOf) {
        return resolveNextStatus(currentStatus, actionType, correctionOf, null);
    }

    /**
     * Resolve and validate the next status for actions that may need external
     * state, currently EXCEPTION_CLOSE restoring the pre-freeze status captured
     * on the snapshot.
     */
    public TraceStatus resolveNextStatus(
            TraceStatus currentStatus,
            ActionType actionType,
            Long correctionOf,
            TraceStatus exceptionRestoreStatus
    ) {
        TraceStatus effectiveCurrentStatus = currentStatus == null ? TraceStatus.INIT : currentStatus;

        if (actionType == null) {
            throw new BizException(BizCode.INVALID_ACTION_TYPE, "actionType 不能为空");
        }

        if (actionType == ActionType.CORRECTION) {
            if (correctionOf == null) {
                throw new BizException(BizCode.PARAM_ERROR, "CORRECTION 类型必须指定 correctionOf 参数");
            }
            return effectiveCurrentStatus;
        }

        if (actionType == ActionType.EXCEPTION_CLOSE) {
            if (correctionOf != null) {
                throw new BizException(BizCode.PARAM_ERROR, "只有 CORRECTION 类型允许指定 correctionOf 参数");
            }
            if (effectiveCurrentStatus != TraceStatus.EXCEPTION) {
                throw new BizException(
                        BizCode.INVALID_ACTION_TYPE,
                        "只有异常冻结状态允许解除冻结: currentStatus="
                                + effectiveCurrentStatus.getCode()
                                + ", actionType=" + actionType.getCode()
                );
            }
            if (exceptionRestoreStatus == null || exceptionRestoreStatus == TraceStatus.EXCEPTION) {
                throw new BizException(BizCode.INVALID_ACTION_TYPE,
                        "异常冻结缺少可恢复的原状态，无法解除冻结");
            }
            return exceptionRestoreStatus;
        }

        if (correctionOf != null) {
            throw new BizException(BizCode.PARAM_ERROR, "只有 CORRECTION 类型允许指定 correctionOf 参数");
        }

        TraceStatus nextStatus = TRANSITIONS
                .getOrDefault(effectiveCurrentStatus, Map.of())
                .get(actionType);
        if (nextStatus == null) {
            throw new BizException(
                    BizCode.INVALID_ACTION_TYPE,
                    "非法状态流转: currentStatus=" + effectiveCurrentStatus.getCode()
                            + ", actionType=" + actionType.getCode()
                            + ", allowedActions=" + allowedActionCodes(effectiveCurrentStatus)
            );
        }
        return nextStatus;
    }

    public boolean canTransit(TraceStatus currentStatus, ActionType actionType) {
        if (actionType == null) {
            return false;
        }
        if (actionType == ActionType.CORRECTION) {
            return true;
        }
        if (actionType == ActionType.EXCEPTION_CLOSE) {
            TraceStatus effectiveCurrentStatus = currentStatus == null ? TraceStatus.INIT : currentStatus;
            return effectiveCurrentStatus == TraceStatus.EXCEPTION;
        }
        TraceStatus effectiveCurrentStatus = currentStatus == null ? TraceStatus.INIT : currentStatus;
        return TRANSITIONS.getOrDefault(effectiveCurrentStatus, Map.of()).containsKey(actionType);
    }

    public List<ActionType> allowedActions(TraceStatus currentStatus) {
        TraceStatus effectiveCurrentStatus = currentStatus == null ? TraceStatus.INIT : currentStatus;
        return TRANSITIONS.getOrDefault(effectiveCurrentStatus, Map.of())
                .keySet()
                .stream()
                // EXCEPTION remains accepted as a legacy alias, but new business
                // suggestions should expose EXCEPTION_OPEN explicitly.
                .filter(actionType -> actionType != ActionType.EXCEPTION)
                .sorted()
                .toList();
    }

    private List<String> allowedActionCodes(TraceStatus currentStatus) {
        return allowedActions(currentStatus).stream()
                .map(ActionType::getCode)
                .toList();
    }

    private static Map<TraceStatus, Map<ActionType, TraceStatus>> createTransitions() {
        EnumMap<TraceStatus, Map<ActionType, TraceStatus>> transitions = new EnumMap<>(TraceStatus.class);

        transitions.put(TraceStatus.INIT, mapOf(
                ActionType.INBOUND, TraceStatus.IN_STOCK,
                ActionType.EXCEPTION, TraceStatus.EXCEPTION,
                ActionType.EXCEPTION_OPEN, TraceStatus.EXCEPTION
        ));
        transitions.put(TraceStatus.IN_STOCK, mapOf(
                ActionType.OUTBOUND, TraceStatus.IN_TRANSIT,
                ActionType.EXCEPTION, TraceStatus.EXCEPTION,
                ActionType.EXCEPTION_OPEN, TraceStatus.EXCEPTION
        ));
        transitions.put(TraceStatus.IN_TRANSIT, mapOf(
                ActionType.TRANSFER, TraceStatus.TRANSFERRED,
                ActionType.INBOUND, TraceStatus.IN_STOCK,
                ActionType.EXCEPTION, TraceStatus.EXCEPTION,
                ActionType.EXCEPTION_OPEN, TraceStatus.EXCEPTION
        ));
        transitions.put(TraceStatus.TRANSFERRED, mapOf(
                ActionType.INBOUND, TraceStatus.IN_STOCK,
                ActionType.EXCEPTION, TraceStatus.EXCEPTION,
                ActionType.EXCEPTION_OPEN, TraceStatus.EXCEPTION
        ));
        // EXCEPTION is treated as a frozen state. Normal inbound/outbound/transfer
        // are blocked; EXCEPTION_CLOSE restores the status captured before freeze.
        transitions.put(TraceStatus.EXCEPTION, mapOf(
                ActionType.EXCEPTION_CLOSE, TraceStatus.EXCEPTION
        ));

        return Collections.unmodifiableMap(transitions);
    }

    private static Map<ActionType, TraceStatus> mapOf(
            ActionType action1, TraceStatus status1,
            ActionType action2, TraceStatus status2
    ) {
        EnumMap<ActionType, TraceStatus> map = new EnumMap<>(ActionType.class);
        map.put(action1, status1);
        map.put(action2, status2);
        return Collections.unmodifiableMap(map);
    }

    private static Map<ActionType, TraceStatus> mapOf(
            ActionType action1, TraceStatus status1,
            ActionType action2, TraceStatus status2,
            ActionType action3, TraceStatus status3
    ) {
        EnumMap<ActionType, TraceStatus> map = new EnumMap<>(ActionType.class);
        map.put(action1, status1);
        map.put(action2, status2);
        map.put(action3, status3);
        return Collections.unmodifiableMap(map);
    }

    private static Map<ActionType, TraceStatus> mapOf(
            ActionType action1, TraceStatus status1
    ) {
        EnumMap<ActionType, TraceStatus> map = new EnumMap<>(ActionType.class);
        map.put(action1, status1);
        return Collections.unmodifiableMap(map);
    }

    private static Map<ActionType, TraceStatus> mapOf(
            ActionType action1, TraceStatus status1,
            ActionType action2, TraceStatus status2,
            ActionType action3, TraceStatus status3,
            ActionType action4, TraceStatus status4
    ) {
        EnumMap<ActionType, TraceStatus> map = new EnumMap<>(ActionType.class);
        map.put(action1, status1);
        map.put(action2, status2);
        map.put(action3, status3);
        map.put(action4, status4);
        return Collections.unmodifiableMap(map);
    }
}
