# T27 Error Code & Exception Convergence

- Task ID: T27
- Created At: 2026-04-14 14:21
- Status: DONE
- Source: `项目整改执行任务表.md`

## Task Definition

Converge backend error-code to HTTP-status mapping and simplify exception throwing style without redesigning the response model.

## Goals

1. Centralize BizCode -> HTTP status inference.
2. Reduce repeated `new BizException(code, status, message)` usage where the status is already deterministic.
3. Keep necessary context-specific overrides explicit.
4. Replace obvious raw runtime failures on the backend error path with business exceptions where appropriate.

## Initial Scope

- `backend/src/main/java/com/example/trace/common/BizCode.java`
- `backend/src/main/java/com/example/trace/common/BizException.java`
- `backend/src/main/java/com/example/trace/common/GlobalExceptionHandler.java`
- `backend/src/main/java/com/example/trace/controller/AuthController.java`
- `backend/src/main/java/com/example/trace/controller/TraceController.java`
- `backend/src/main/java/com/example/trace/service/impl/support/TraceCodeAssignmentService.java`
- `backend/src/main/java/com/example/trace/service/impl/support/TraceScanRetryExecutor.java`
- `backend/src/main/java/com/example/trace/service/impl/support/TraceScanTransactionService.java`
- `backend/src/main/java/com/example/trace/util/SignatureUtil.java`
- Focused backend tests only

## Out of Scope

- `ApiResponse` factory redesign (belongs to T28)
- Broad frontend contract adjustments
- New domain error-code taxonomy expansion beyond low-risk convergence

## Final Result

- `BizCode.httpStatusOf(...)` is now the single default mapping entry.
- `BizException` delegates to that mapping and supports cause-preserving construction.
- Auth / trace / scan helper call sites now prefer inferred-status constructors where semantics are stable.
- `PASSWORD_ERROR` in change-password old-password mismatch intentionally remains explicit `400`.
- `SignatureUtil` now throws `BizException` for init/sign failures.
