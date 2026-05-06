---
name: t25-permission-service-split
description: Continue T25 PermissionService responsibility split work in d:\bysj. Use when the user asks to continue T25, PermissionService refactoring, permission-chain cleanup, permission cache or matcher splitting, or related backend security and test work. Read docs/progress/MASTER.md first, keep frontend UI files frozen, and update progress docs plus the root task table markdown file after each completed subtask.
---

# T25 PermissionService Split

## Startup Checklist

1. Read the root task table markdown file in the repository root.
2. Read `docs/progress/MASTER.md`.
3. Read the active phase file linked from `MASTER.md`.
4. Read the current task spec, analysis, and plan docs relevant to T25:
   - `docs/specs/2026-04-14-t25-permission-service-split.md`
   - `docs/analysis/project-overview.md`
   - `docs/analysis/module-inventory.md`
   - `docs/analysis/risk-assessment.md`
   - `docs/plan/task-breakdown.md`
5. Report the current task, current phase, completed items, pending items, and next planned subtask.

## Hard Constraints

- Do not continue the old `docs/superpowers/*` workflow for T25 except as historical reference.
- Do not edit frontend view or component files unless the user explicitly re-authorizes them in the current conversation.
- Keep scope inside T25: PermissionService responsibility split, contracts, cache, tests, and caller integration.
- Prefer test-first or test-locking changes before structural refactors.
- Keep `PermissionService` external behavior compatible until the plan explicitly allows a call-site change.

## Execution Workflow

### Phase 1

- Lock current behavior with focused backend tests.
- Confirm the collaborator split before moving large logic.

### Phase 2

- Extract permission-code read path and inheritance expansion.
- Extract API-permission read path and method/path matcher.

### Phase 3

- Collapse `PermissionService` into a thin facade or coordinator.
- Re-check callers:
  - `LoginInterceptor`
  - `PermissionInterceptor`
  - `AuthController`
  - `TraceController`
  - `RoleServiceImpl`

### Phase 4

- Run focused regression tests.
- Update all progress artifacts.
- Mark T25 done only after docs and verification are both complete.

## Progress Update Rules

After every completed subtask:

1. Check the corresponding item in the active phase file.
2. Update the completion count in `docs/progress/MASTER.md`.
3. Update `Current Status` and `Next Steps` in `docs/progress/MASTER.md`.
4. Update the root task table markdown file:
   - current in-progress task section
   - latest update summary section
   - task status overview table
   - update record section
5. Record any blocker or design decision in the phase file notes.

## Parallel Lane Protocol

- If the user explicitly authorizes sub-agents in a future conversation, only parallelize tasks from different lanes with disjoint write scopes.
- Without explicit user authorization, execute all tasks sequentially yourself.
- Even when lanes exist in the plan, do not force parallelism.

## Validation Rules

- Prefer focused backend tests first, then broader regression if needed.
- Do not mark a phase complete without evidence from code or tests.
- Do not treat documentation-only progress as implementation complete.

## Completion Trigger

- When every checkbox in `docs/progress/MASTER.md` is complete, announce that T25 is finished.
- Before any cleanup or removal of generated docs or this skill, ask the user what to keep.
