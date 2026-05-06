package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.entity.TraceCode;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceCodeStatus;
import com.example.trace.enums.TraceStatus;
import com.example.trace.mapper.TraceCodeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceCodeStatusServiceTest {

    @Mock
    private TraceCodeMapper traceCodeMapper;

    private TraceCodeStatusService service;

    @BeforeEach
    void setUp() {
        service = new TraceCodeStatusService(traceCodeMapper);
    }

    @Test
    void createGeneratedCode_shouldPersistInitialSingleItemCodeStatus() {
        TraceCode code = service.createGeneratedCode(new TraceCodeStatusService.CreateCommand(
                " TRACE-001 ",
                10L,
                1L,
                3,
                " payload ",
                "TRACE-001"
        ));

        ArgumentCaptor<TraceCode> codeCaptor = ArgumentCaptor.forClass(TraceCode.class);
        verify(traceCodeMapper).insert(codeCaptor.capture());
        TraceCode persisted = codeCaptor.getValue();
        assertThat(code).isSameAs(persisted);
        assertThat(persisted.getTraceCode()).isEqualTo("TRACE-001");
        assertThat(persisted.getBatchId()).isEqualTo(10L);
        assertThat(persisted.getSpuId()).isEqualTo(1L);
        assertThat(persisted.getSerialNo()).isEqualTo(3);
        assertThat(persisted.getQrPayload()).isEqualTo("payload");
        assertThat(persisted.getCodeStatus()).isEqualTo(TraceCodeStatus.GENERATED.name());
        assertThat(persisted.getPrintCount()).isZero();
        assertThat(persisted.getCurrentSnapshotId()).isEqualTo("TRACE-001");
    }

    @Test
    void movementEligibility_shouldKeepLegacySnapshotOnlyCodesAllowed() {
        when(traceCodeMapper.selectById("TRACE-LEGACY")).thenReturn(null);

        TraceCodeStatusService.MovementEligibility eligibility =
                service.movementEligibility("TRACE-LEGACY");

        assertThat(eligibility.blocked()).isFalse();
        assertThat(eligibility.status()).isNull();
    }

    @Test
    void ensureLifecycleMovementAllowed_shouldRejectGeneratedAndPrintedCodes() {
        when(traceCodeMapper.selectById("TRACE-GEN"))
                .thenReturn(code("TRACE-GEN", TraceCodeStatus.GENERATED));

        assertThatThrownBy(() -> service.ensureLifecycleMovementAllowed("TRACE-GEN", ActionType.INBOUND))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.INVALID_ACTION_TYPE);
                    assertThat(exception.getMessage()).contains("GENERATED").contains("尚未激活");
                });

        verify(traceCodeMapper, never()).updateById(any(TraceCode.class));
    }

    @Test
    void ensureLifecycleMovementAllowed_shouldIgnoreNonMovementAuditActions() {
        service.ensureLifecycleMovementAllowed("TRACE-GEN", ActionType.CORRECTION);

        verify(traceCodeMapper, never()).selectById("TRACE-GEN");
    }

    @Test
    void markActivated_shouldMoveGeneratedOrPrintedCodeToActivated() {
        TraceCode code = code("TRACE-PRINTED", TraceCodeStatus.PRINTED);
        when(traceCodeMapper.selectById("TRACE-PRINTED")).thenReturn(code);
        LocalDateTime activatedTime = LocalDateTime.of(2026, 5, 5, 20, 0);

        TraceCode returned = service.markActivated("TRACE-PRINTED", 7L, " producer ", activatedTime);

        ArgumentCaptor<TraceCode> codeCaptor = ArgumentCaptor.forClass(TraceCode.class);
        verify(traceCodeMapper).updateById(codeCaptor.capture());
        TraceCode updated = codeCaptor.getValue();
        assertThat(returned).isSameAs(updated);
        assertThat(updated.getCodeStatus()).isEqualTo(TraceCodeStatus.ACTIVATED.name());
        assertThat(updated.getActivatedBy()).isEqualTo(7L);
        assertThat(updated.getActivatedByUsername()).isEqualTo("producer");
        assertThat(updated.getActivatedTime()).isEqualTo(activatedTime);
        assertThat(updated.getCurrentSnapshotId()).isEqualTo("TRACE-PRINTED");

        service.ensureLifecycleMovementAllowed("TRACE-PRINTED", ActionType.INBOUND);
    }

    @Test
    void markPrinted_shouldOnlyPrintGeneratedCode() {
        TraceCode code = code("TRACE-GEN", TraceCodeStatus.GENERATED);
        when(traceCodeMapper.selectById("TRACE-GEN")).thenReturn(code);

        TraceCode updated = service.markPrinted("TRACE-GEN");

        assertThat(updated.getCodeStatus()).isEqualTo(TraceCodeStatus.PRINTED.name());
        assertThat(updated.getPrintCount()).isEqualTo(1);
        verify(traceCodeMapper).updateById(code);
    }

    @Test
    void markReprinted_shouldIncrementPrintCountWithoutRollingBackStatus() {
        TraceCode code = code("TRACE-ACTIVE", TraceCodeStatus.ACTIVATED);
        code.setPrintCount(2);
        when(traceCodeMapper.selectById("TRACE-ACTIVE")).thenReturn(code);

        TraceCode updated = service.markReprinted("TRACE-ACTIVE");

        assertThat(updated.getCodeStatus()).isEqualTo(TraceCodeStatus.ACTIVATED.name());
        assertThat(updated.getPrintCount()).isEqualTo(3);
        verify(traceCodeMapper).updateById(code);
    }

    @Test
    void markReprinted_shouldRejectNeverPrintedGeneratedCode() {
        when(traceCodeMapper.selectById("TRACE-GEN"))
                .thenReturn(code("TRACE-GEN", TraceCodeStatus.GENERATED));

        assertThatThrownBy(() -> service.markReprinted("TRACE-GEN"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(BizCode.INVALID_ACTION_TYPE));
    }

    @Test
    void syncAfterLifecycleTransition_shouldMirrorSnapshotStatusToCodeStatus() {
        TraceCode code = code("TRACE-STOCK", TraceCodeStatus.ACTIVATED);
        when(traceCodeMapper.selectById("TRACE-STOCK")).thenReturn(code);

        service.syncAfterLifecycleTransition("TRACE-STOCK", TraceStatus.IN_STOCK);

        ArgumentCaptor<TraceCode> codeCaptor = ArgumentCaptor.forClass(TraceCode.class);
        verify(traceCodeMapper).updateById(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getCodeStatus()).isEqualTo(TraceCodeStatus.IN_STOCK.name());
        assertThat(codeCaptor.getValue().getCurrentSnapshotId()).isEqualTo("TRACE-STOCK");
    }

    @Test
    void markVoided_shouldRejectAlreadyActivatedCode() {
        when(traceCodeMapper.selectById("TRACE-ACTIVE"))
                .thenReturn(code("TRACE-ACTIVE", TraceCodeStatus.ACTIVATED));

        assertThatThrownBy(() -> service.markVoided("TRACE-ACTIVE"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(BizCode.INVALID_ACTION_TYPE));
    }

    @Test
    void markVoided_shouldMoveGeneratedCodeToVoided() {
        TraceCode code = code("TRACE-GEN", TraceCodeStatus.GENERATED);
        when(traceCodeMapper.selectById("TRACE-GEN")).thenReturn(code);

        TraceCode updated = service.markVoided("TRACE-GEN");

        assertThat(updated.getCodeStatus()).isEqualTo(TraceCodeStatus.VOIDED.name());
        verify(traceCodeMapper).updateById(code);
    }

    @Test
    void voidedCode_shouldNotActivateOrEnterLifecycleMovement() {
        when(traceCodeMapper.selectById("TRACE-VOIDED"))
                .thenReturn(code("TRACE-VOIDED", TraceCodeStatus.VOIDED));

        assertThatThrownBy(() -> service.markActivated("TRACE-VOIDED", 7L, "producer", null))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(BizCode.INVALID_ACTION_TYPE));

        assertThatThrownBy(() -> service.ensureLifecycleMovementAllowed("TRACE-VOIDED", ActionType.INBOUND))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.INVALID_ACTION_TYPE);
                    assertThat(exception.getMessage()).contains("VOIDED");
                });
    }

    private static TraceCode code(String traceCode, TraceCodeStatus status) {
        TraceCode code = new TraceCode();
        code.setTraceCode(traceCode);
        code.setSpuId(1L);
        code.setCodeStatus(status.name());
        code.setPrintCount(0);
        return code;
    }
}
