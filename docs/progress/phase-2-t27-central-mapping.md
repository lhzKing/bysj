# Phase 2: Central Mapping Convergence

- [x] Add centralized HTTP status inference to `BizCode`.
- [x] Delegate `BizException` status inference to `BizCode`.
- [x] Keep explicit override construction for exceptional cases.

## Notes

- Added `BizCode.httpStatusOf(...)` as the default mapping entry.
- `BizException` now supports both inferred-status and cause-preserving construction.
- Explicit override constructor remains for intentional deviations such as `PASSWORD_ERROR -> 400` in change-password validation.
