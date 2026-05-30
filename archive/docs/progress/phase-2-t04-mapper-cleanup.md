# Phase 2: Mapper Cleanup

- [x] Remove stale custom method from `TraceSnapshotMapper`.
- [x] Remove stale XML mapping file if it no longer contains any active SQL.

## Notes

- Removed `selectByTraceCodeForUpdate(...)` because the project already uses optimistic locking for `TraceSnapshot`.
- Deleted `backend/src/main/resources/mapper/TraceMapper.xml`; it only backed the stale custom method.
