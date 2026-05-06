package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.enums.ActionType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceScanRetryExecutorTest {

    @Test
    void execute_shouldRetryUntilSuccess() {
        AtomicInteger attempts = new AtomicInteger();
        TraceScanRetryExecutor executor = new TraceScanRetryExecutor((request, operator) -> {
            if (attempts.getAndIncrement() < 2) {
                throw new TraceOptimisticLockException("conflict");
            }
        });

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);

        executor.execute(request, "tester");

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void execute_shouldThrowBizExceptionAfterMaxRetryExceeded() {
        TraceScanRetryExecutor executor = new TraceScanRetryExecutor((request, operator) -> {
            throw new TraceOptimisticLockException("conflict");
        });

        ScanTraceRequest request = new ScanTraceRequest();
        request.setTraceCode("trace-1");
        request.setActionType(ActionType.INBOUND);

        assertThatThrownBy(() -> executor.execute(request, "tester"))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getCode())
                .isEqualTo(BizCode.CONCURRENT_CONFLICT);
    }
}
