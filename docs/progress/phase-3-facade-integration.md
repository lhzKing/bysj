# Phase 3: Facade Integration

## Goal

Keep `PermissionService` as the stable facade while integrating the newly split collaborators into application callers.

## Parallel Lane

- Lane 1 only; Merge Risk = Medium

## Tasks

- [x] T25-3.1 Rebuild `PermissionService` as a facade over query, inheritance, API matching, and cache collaborators.
- [x] T25-3.2 Confirm `LoginInterceptor`, `PermissionInterceptor`, `AuthController`, `TraceController`, and `RoleServiceImpl` still use the stable facade contract.

## Notes

- 2026-04-14 03:51: `PermissionService` now delegates to `RolePermissionQueryService`, `PermissionInheritanceResolver`, `ApiPermissionMatcher`, and `PermissionCache`.
- 2026-04-14 03:51: `AuthController`, `TraceController`, and `RoleServiceImpl` continue to depend on the facade instead of individual collaborators.
- 2026-04-14 03:51: `LoginInterceptorTest` and `PermissionInterceptorTest` covered login permission loading and API fallback behavior.
