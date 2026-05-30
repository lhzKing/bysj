# T29 Historical Residue & Invalid Constant Cleanup

- Task ID: T29
- Created At: 2026-04-14 16:13
- Status: DONE
- Source: `项目整改执行任务表.md`

## Task Definition

Clean low-risk historical residue, invalid compatibility methods, and old interfaces that are no longer used in the repository.

## Goals

1. Remove clearly unused compatibility stubs and old interfaces.
2. Remove old constructors / helpers that only preserve historical call patterns inside the repository.
3. Keep cleanup scoped to zero-reference or directly migrated residue.
4. Preserve current runtime behavior for actively used flows.

## Scope

- `backend/src/main/java/com/example/trace/security/TokenStore.java`
- `backend/src/main/java/com/example/trace/controller/TraceController.java`
- `backend/src/main/java/com/example/trace/dto/LoginResponse.java`
- `backend/src/main/java/com/example/trace/common/ApiResponse.java`
- `backend/src/main/java/com/example/trace/controller/AuthController.java`

## Final Result

- Removed unused legacy static compatibility methods from `TokenStore`.
- Removed unused legacy trace endpoints from `TraceController` after repository usage audit.
- Removed the old 3-arg `LoginResponse` constructor after migrating callers to the explicit permissions-bearing constructor.
- Removed the unused deprecated `ApiResponse.fail(String)` alias.
- Confirmed current frontend trace API already uses the new RESTful endpoints.
