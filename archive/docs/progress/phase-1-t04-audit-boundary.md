# Phase 1: Audit & Boundary Lock

- [x] Inspect `TraceSnapshotMapper`, `TraceMapper.xml`, and active call sites.
- [x] Confirm delete-not-revive strategy based on optimistic locking.

## Notes

- `TraceScanTransactionService` uses `selectById(...)` and `updateById(...)` with `@Version`.
- `TraceMapper.xml` contains only `selectByTraceCodeForUpdate(...)`.
- No live caller references the custom method.
