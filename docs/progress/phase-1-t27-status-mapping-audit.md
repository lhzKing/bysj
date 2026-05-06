# Phase 1: Status Mapping Audit

- [x] Inspect `BizCode`, `BizException`, and `GlobalExceptionHandler`.
- [x] Inventory repeated explicit `BizException(code, status, message)` hotspots.
- [x] Record intentional override cases to preserve.

## Notes

- Existing `BizException.inferHttpStatus(...)` does not cover all declared business codes.
- `AuthController` mixes default-style and explicit-status-style exception throwing.
- `PASSWORD_ERROR` needs one intentional divergence: login-like failures stay `401`, while old-password mismatch in `changePassword` remains explicit `400`.
- `SignatureUtil` still throws raw `RuntimeException` on init/sign failure.
