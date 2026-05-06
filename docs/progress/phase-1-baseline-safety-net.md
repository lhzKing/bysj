# Phase 1: Baseline & Safety Net

## Goal

Lock the existing `PermissionService` behavior with tests before splitting responsibilities.

## Parallel Lane

- Lane 1 only; Merge Risk = Low

## Tasks

- [x] T25-1.1 Add baseline tests covering `matchAll` / `matchAny`, API method/path matching, and `roleCode -> roleId` hit/miss/null paths.
- [x] T25-1.2 Document the current facade behavior so the later split keeps `PermissionService` as the stable entry point.

## Notes

- 2026-04-14 02:53: Spec-driven development generated the Phase 1 task breakdown.
- 2026-04-14 03:36: `PermissionServiceTest` added 7 regression cases for permission matching, API fallback, and role lookup behavior.
- 2026-04-14 03:36: Identified target collaborators: `PermissionInheritanceResolver`, `ApiPermissionMatcher`, `PermissionCache`, `RolePermissionQueryService`, and the `PermissionService` facade.
- 2026-04-14 03:36: Verification passed via `cd backend && mvn test "-Dtest=PermissionServiceTest"` (7 tests).
