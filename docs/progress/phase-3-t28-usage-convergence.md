# Phase 3: Usage Convergence

- [x] Replace obvious `ApiResponse.ok(...)` usage with canonical success factory.
- [x] Reuse canonical failure factory where natural.
- [x] Avoid payload-contract changes.

## Notes

- `AuthController`, `TraceController`, and `DashboardController` now prefer `ApiResponse.success(...)` for 200 responses.
- `GlobalExceptionHandler` framework-path fallback now builds inferred failure responses via `ApiResponse.fail(code, message)`.
- `LoginInterceptor` / `PermissionInterceptor` now use the canonical failure factory path for 401/403 payload construction.
- No response field shape changes were introduced.
