# Phase 2: Annotation Mapper Convergence

- [x] Refactor `SysUserMapper` to reuse shared mapping definitions.

## Notes

- Introduced `USER_WITH_ROLE_BASE_SELECT` to avoid duplicated query text.
- Added `@Results(id = "sysUserWithRoleResultMap", ...)` and reused it through `@ResultMap`.
