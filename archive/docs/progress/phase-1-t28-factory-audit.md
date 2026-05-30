# Phase 1: Factory Audit

- [x] Inspect duplicated `ApiResponse` success and failure factories.
- [x] Inventory controller / exception-handler usage hotspots.
- [x] Lock low-risk T28 scope.

## Notes

- `success(...)` and `ok(...)` are semantically identical 200-success aliases.
- Failure factories duplicate literal code/status pairs that can delegate to one canonical builder.
- T28 will preserve the explicit `fail(code, status, message)` path introduced/used by T27.
