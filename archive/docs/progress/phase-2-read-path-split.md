# Phase 2: Read Path Split

## Goal

Split `PermissionService` read paths into permission inheritance, role-permission query, API matching, and cache responsibilities.

## Parallel Lane

- Lane A: permission inheritance and query split; Merge Risk = Medium
- Lane B: API matcher split; Merge Risk = Medium
- Coordinate carefully around the shared `PermissionService` facade.

## Tasks

- [x] T25-2.1 Extract role-permission query and inheritance resolution while preserving `getPermissionCodes` behavior.
- [x] T25-2.2 Extract API permission matching while preserving `hasApiPermission` wildcard and method matching behavior.

## Notes

- 2026-04-14 03:47: Added `PermissionInheritanceResolver` for inherited permission expansion.
- 2026-04-14 03:47: Added `ApiPermissionMatcher` for API method/path matching.
- 2026-04-14 03:47: Added `PermissionCache` and `RolePermissionQueryService`, then rewired `PermissionService` to delegate read-path work.
- 2026-04-14 03:47: Focused tests for `PermissionInheritanceResolverTest` and `ApiPermissionMatcherTest` passed.
