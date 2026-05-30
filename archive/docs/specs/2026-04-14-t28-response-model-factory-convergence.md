# T28 Response Model Factory Convergence

- Task ID: T28
- Created At: 2026-04-14 15:27
- Status: DONE
- Source: `项目整改执行任务表.md`

## Task Definition

Converge `ApiResponse` factory methods and standardize backend success/error response construction without changing the response payload contract.

## Goals

1. Reduce duplicated factory logic inside `ApiResponse`.
2. Make one canonical success factory path and one canonical failure factory path.
3. Keep backward compatibility for existing response shape and low-risk aliases.
4. Standardize obvious controller/interceptor usage to the canonical factories.

## Initial Scope

- `backend/src/main/java/com/example/trace/common/ApiResponse.java`
- `backend/src/main/java/com/example/trace/common/GlobalExceptionHandler.java`
- `backend/src/main/java/com/example/trace/controller/AuthController.java`
- `backend/src/main/java/com/example/trace/controller/TraceController.java`
- `backend/src/main/java/com/example/trace/controller/DashboardController.java`
- Focused response-related tests

## Out of Scope

- Frontend request/response adapter changes
- New API fields or payload shape redesign
- Controller-wide stylistic refactor beyond obvious response factory convergence

## Final Result

- `ApiResponse` now has one canonical success builder path and one canonical failure builder path.
- `success(...)` is the preferred 200-success factory; `ok(...)` remains as a compatible deprecated alias.
- `fail(code, message)` now infers status from `BizCode`, while `fail(code, status, message)` remains available for intentional override cases.
- Auth / Trace / Dashboard mainline responses now use the canonical success factory.
- Login / permission interceptors and framework-error handling now reuse the converged failure path where appropriate.
