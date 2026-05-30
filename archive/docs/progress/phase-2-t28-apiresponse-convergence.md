# Phase 2: ApiResponse Factory Convergence

- [x] Extract canonical success factory path.
- [x] Extract canonical failure factory path.
- [x] Keep explicit-status override path and low-risk aliases.

## Notes

- Added shared internal builders (`of`, success-path helper, failure-path helper) to `ApiResponse`.
- `success(...)` is now the canonical 200-success path; `created(...)` keeps 201 semantics through the shared success builder.
- Added inferred `fail(code, message)` while preserving explicit `fail(code, status, message)` for override scenarios from T27.
- Compatibility aliases (`ok(...)`, convenience failure helpers) now delegate to the canonical paths.
