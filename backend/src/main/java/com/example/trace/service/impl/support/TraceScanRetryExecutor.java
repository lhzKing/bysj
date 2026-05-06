package com.example.trace.service.impl.support;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ScanTraceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TraceScanRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(TraceScanRetryExecutor.class);
    private static final int MAX_RETRY_COUNT = 3;

    private final TraceScanExecutor traceScanExecutor;

    public TraceScanRetryExecutor(TraceScanExecutor traceScanExecutor) {
        this.traceScanExecutor = traceScanExecutor;
    }

    public void execute(ScanTraceRequest request, String operator) {
        executeAndReturnCreated(request, operator);
    }

    public boolean executeAndReturnCreated(ScanTraceRequest request, String operator) {
        int retryCount = 0;
        while (true) {
            try {
                return traceScanExecutor.executeAndReturnCreated(request, operator);
            } catch (TraceOptimisticLockException e) {
                retryCount++;
                if (retryCount > MAX_RETRY_COUNT) {
                    log.error("乐观锁重试{}次后仍失败，traceCode: {}", MAX_RETRY_COUNT, request.getTraceCode());
                    throw new BizException(BizCode.CONCURRENT_CONFLICT, "并发冲突，请稍后重试");
                }
                log.warn("乐观锁冲突，第{}次重试，traceCode: {}", retryCount, request.getTraceCode());
                try {
                    Thread.sleep(50L * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BizException(BizCode.SERVER_ERROR, "操作被中断");
                }
            }
        }
    }
}
