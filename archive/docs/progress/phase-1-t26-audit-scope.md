# Phase 1: Audit & Scope Lock

- [x] Inspect mapper duplication hotspots.
- [x] Keep scope limited to low-risk convergence.

## Notes

- `SysUserMapper` duplicates both SQL and result mapping definitions.
- `TraceLifecycleLogMapper.xml` repeats lifecycle-log columns across multiple selects.
- XML-vs-annotation style itself is not being globally standardized in this task.
