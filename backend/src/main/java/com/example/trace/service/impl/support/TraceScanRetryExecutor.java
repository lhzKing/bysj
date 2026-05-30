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
    // 同码并发扫码通常是短时间冲突，最多重试 3 次，避免用户请求无限等待。
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
                /*
                 * 关键设计：重试循环放在“无事务”的外层。
                 *
                 * 真正的数据库写入在 TraceScanTransactionService 的 REQUIRES_NEW 事务里完成。
                 * 如果把 while 循环写进同一个事务，在可重复读隔离级别下重试仍可能读到旧快照，
                 * 导致永远拿不到别人提交后的新 version。
                 */
                return traceScanExecutor.executeAndReturnCreated(request, operator);
            } catch (TraceOptimisticLockException e) {
                retryCount++;
                if (retryCount > MAX_RETRY_COUNT) {
                    log.error("乐观锁重试{}次后仍失败，traceCode: {}", MAX_RETRY_COUNT, request.getTraceCode());
                    throw new BizException(BizCode.CONCURRENT_CONFLICT, "并发冲突，请稍后重试");
                }
                log.warn("乐观锁冲突，第{}次重试，traceCode: {}", retryCount, request.getTraceCode());
                try {
                    // 线性退避：第 1/2/3 次分别等待 50/100/150ms，给并发事务提交留出时间。
                    Thread.sleep(50L * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BizException(BizCode.SERVER_ERROR, "操作被中断");
                }
            }
        }
    }
}
