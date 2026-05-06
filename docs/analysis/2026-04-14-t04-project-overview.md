# T04 Project Overview

## Background

- Workspace: `d:\bysj`
- Current active non-UI remediation task: T04
- Previous completed mainline task: T21
- Current one-off frontend exception work already closed and documented separately

## Relevant Runtime Path

1. `TraceServiceImpl.detail(traceCode)` reads snapshot by `selectById(traceCode)`.
2. `TraceScanTransactionService.execute(...)` reads snapshot by `selectById(traceCode)` and updates via optimistic lock.
3. `TraceScanRetryExecutor` retries when `TraceOptimisticLockException` is thrown.
4. `MybatisPlusConfig` explicitly enables `OptimisticLockerInnerInterceptor`.

## T04 Conclusion

The current codebase already standardized on optimistic locking for `TraceSnapshot`. The custom
`FOR UPDATE` mapper method is not part of the live flow and should likely be removed together with its XML.
