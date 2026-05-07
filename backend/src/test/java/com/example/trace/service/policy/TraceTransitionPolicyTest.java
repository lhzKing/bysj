package com.example.trace.service.policy;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceTransitionPolicyTest {

    private final TraceTransitionPolicy policy = new TraceTransitionPolicy();

    @Test
    void resolveNextStatus_shouldAllowMinimalHappyPathTransitions() {
        assertThat(policy.resolveNextStatus(TraceStatus.INIT, ActionType.INBOUND, null))
                .isEqualTo(TraceStatus.IN_STOCK);
        assertThat(policy.resolveNextStatus(TraceStatus.IN_STOCK, ActionType.OUTBOUND, null))
                .isEqualTo(TraceStatus.IN_TRANSIT);
        assertThat(policy.resolveNextStatus(TraceStatus.IN_TRANSIT, ActionType.TRANSFER, null))
                .isEqualTo(TraceStatus.TRANSFERRED);
        assertThat(policy.resolveNextStatus(TraceStatus.TRANSFERRED, ActionType.INBOUND, null))
                .isEqualTo(TraceStatus.IN_STOCK);
    }

    @Test
    void resolveNextStatus_shouldRejectIllegalTransition() {
        assertThatThrownBy(() -> policy.resolveNextStatus(TraceStatus.INIT, ActionType.OUTBOUND, null))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.INVALID_ACTION_TYPE);
                    assertThat(exception.getMessage()).contains("currentStatus=INIT");
                    assertThat(exception.getMessage()).contains("actionType=OUTBOUND");
                });
    }

    @Test
    void resolveNextStatus_shouldFreezeExceptionStateForNormalActions() {
        assertThatThrownBy(() -> policy.resolveNextStatus(TraceStatus.EXCEPTION, ActionType.INBOUND, null))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(BizCode.INVALID_ACTION_TYPE));
    }

    @Test
    void resolveNextStatus_shouldOpenAndCloseExceptionHoldWithRestoreStatus() {
        assertThat(policy.resolveNextStatus(TraceStatus.IN_STOCK, ActionType.EXCEPTION_OPEN, null))
                .isEqualTo(TraceStatus.EXCEPTION);
        assertThat(policy.resolveNextStatus(TraceStatus.IN_STOCK, ActionType.EXCEPTION, null))
                .as("legacy EXCEPTION remains accepted as an EXCEPTION_OPEN alias")
                .isEqualTo(TraceStatus.EXCEPTION);
        assertThat(policy.resolveNextStatus(
                TraceStatus.EXCEPTION,
                ActionType.EXCEPTION_CLOSE,
                null,
                TraceStatus.IN_STOCK
        )).isEqualTo(TraceStatus.IN_STOCK);
    }

    @Test
    void resolveNextStatus_shouldRejectExceptionCloseWithoutRestoreStatus() {
        assertThatThrownBy(() -> policy.resolveNextStatus(
                TraceStatus.EXCEPTION,
                ActionType.EXCEPTION_CLOSE,
                null,
                null
        ))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(BizCode.INVALID_ACTION_TYPE));
    }

    @Test
    void resolveNextStatus_shouldKeepStatusForCorrection() {
        assertThat(policy.resolveNextStatus(TraceStatus.EXCEPTION, ActionType.CORRECTION, 100L))
                .isEqualTo(TraceStatus.EXCEPTION);
        assertThat(policy.resolveNextStatus(TraceStatus.IN_TRANSIT, ActionType.CORRECTION, 100L))
                .isEqualTo(TraceStatus.IN_TRANSIT);
    }

    @Test
    void resolveNextStatus_shouldRequireCorrectionTargetForCorrection() {
        assertThatThrownBy(() -> policy.resolveNextStatus(TraceStatus.IN_STOCK, ActionType.CORRECTION, null))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(BizCode.PARAM_ERROR));
    }

    @Test
    void allowedActions_shouldExposeStateSpecificNormalActions() {
        assertThat(policy.allowedActions(TraceStatus.IN_TRANSIT))
                .containsExactly(ActionType.INBOUND, ActionType.TRANSFER, ActionType.EXCEPTION_OPEN);
        assertThat(policy.allowedActions(TraceStatus.EXCEPTION)).containsExactly(ActionType.EXCEPTION_CLOSE);
        assertThat(policy.allowedActions(TraceStatus.IN_STOCK))
                .containsExactly(ActionType.OUTBOUND, ActionType.EXCEPTION_OPEN)
                .doesNotContain(ActionType.EXCEPTION);
    }

    @Test
    void canTransit_shouldReflectFrozenAndAuditOnlyBranches() {
        assertThat(policy.canTransit(TraceStatus.IN_STOCK, ActionType.OUTBOUND)).isTrue();
        assertThat(policy.canTransit(TraceStatus.EXCEPTION, ActionType.INBOUND)).isFalse();
        assertThat(policy.canTransit(TraceStatus.EXCEPTION, ActionType.EXCEPTION_CLOSE)).isTrue();
        assertThat(policy.canTransit(TraceStatus.IN_TRANSIT, ActionType.CORRECTION)).isTrue();
        assertThat(policy.canTransit(TraceStatus.IN_STOCK, null)).isFalse();
    }
}
