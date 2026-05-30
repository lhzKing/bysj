# T26 Mapper Style Convergence

- Task ID: T26
- Created At: 2026-04-14 11:40
- Status: DONE
- Source: `项目整改执行任务表.md`

## Task Definition

Converge mapper style and remove duplicated mapping definitions without changing runtime behavior.

## Goals

1. Reduce duplicated mapper SQL/result mapping definitions.
2. Keep simple mappers simple and only refactor obvious duplication.
3. Avoid broad rewrites from annotation style to XML style or vice versa.
4. Preserve current service contracts and test behavior.

## Initial Scope

- `SysUserMapper` duplicated `@Select` + `@Results`
- `TraceLifecycleLogMapper.xml` duplicated lifecycle-log column projections
- No frontend changes
