# Phase 3: Call-site Convergence

- [x] Refactor obvious deterministic exception call sites to inferred mapping.
- [x] Preserve context-specific overrides where semantics intentionally differ.
- [x] Replace `SignatureUtil` raw runtime failures with business exceptions.

## Notes

- `AuthController`, `TraceController`, `TraceCodeAssignmentService`, `TraceScanRetryExecutor`, and `TraceScanTransactionService` now prefer inferred-status constructors for stable mappings.
- `AuthController.changePassword(...)` intentionally keeps `PASSWORD_ERROR` old-password mismatch as explicit `400`.
- `USER_EXISTS` now follows duplicate-resource conflict semantics via the default `409` mapping.
- `SignatureUtil` init/sign failures now surface as `BizException(BizCode.SERVER_ERROR, ...)`.
