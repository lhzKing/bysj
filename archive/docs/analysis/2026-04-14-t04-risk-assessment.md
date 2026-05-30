# T04 Risk Assessment

## Risk 1: Removing XML could break mapper bootstrapping

- Mitigation: confirm `TraceMapper.xml` only contains the stale method and run focused backend tests.

## Risk 2: A hidden caller may still depend on the custom method

- Mitigation: grep the codebase for `selectByTraceCodeForUpdate` before removal.

## Risk 3: Cleanup could accidentally change concurrency semantics

- Mitigation: preserve the current `selectById + @Version + retry` path exactly as-is.

## Recommended Resolution

Delete the unused mapper method and its XML mapping, because the project already standardized on optimistic locking.
