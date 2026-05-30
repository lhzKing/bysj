# Phase 3: Old Interface Cleanup

- [x] Remove unused deprecated legacy trace endpoints.
- [x] Keep active RESTful endpoints unchanged.
- [x] Verify no in-repo callers remain for removed interfaces.

## Notes

- Removed deprecated `TraceController` aliases for `/api/traces/produce/assign`, `/api/traces/scan`, and `/api/traces/detail/{traceCode}`.
- Whole-repo search found no current frontend or backend callers for those legacy paths.
- Active endpoints (`/api/traces`, `/api/traces/{traceCode}/events`, `/api/traces/{traceCode}`) remain unchanged.
