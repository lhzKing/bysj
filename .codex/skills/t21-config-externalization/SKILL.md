---
name: t21-config-externalization
description: Continue T21 config externalization work in d:\bysj. Use when the user asks to continue T21, configuration governance, env externalization, CORS config cleanup, Vite dev/proxy config cleanup, or related backend/frontend config work. Read docs/progress/MASTER.md first, keep frontend UI files frozen, and update progress docs plus the root task table markdown file after each completed subtask.
---

# T21 Config Externalization

## Startup Checklist

1. Read the root task table markdown file in the repository root.
2. Read `docs/progress/MASTER.md`.
3. Read the active phase file linked from `MASTER.md`.
4. Read the current T21 documents:
   - `docs/specs/2026-04-14-t21-config-externalization.md`
   - `docs/analysis/2026-04-14-t21-config-project-overview.md`
   - `docs/analysis/2026-04-14-t21-config-module-inventory.md`
   - `docs/analysis/2026-04-14-t21-config-risk-assessment.md`
   - `docs/plan/2026-04-14-t21-task-breakdown.md`
5. Report the current task, current phase, completed items, pending items, and next planned subtask.

## Hard Constraints

- Do not continue the old `docs/superpowers/*` workflow for T21 except as historical reference.
- Do not edit frontend view or component files unless the user explicitly re-authorizes them in the current conversation.
- Keep scope inside T21: backend runtime config, CORS config unification, frontend Vite/dev config, env templates, and related verification.
- Treat map key component wiring as deferred to T23 unless the user explicitly lifts the UI-file freeze.

## Execution Workflow

### Phase 1

- Audit config surfaces and confirm scope boundaries.
- Record frozen-file blockers before coding.

### Phase 2

- Externalize backend runtime config in `application.yml`.
- Unify CORS filter and MVC config onto shared config properties.

### Phase 3

- Externalize Vite dev server / HTTPS / cert / proxy config.
- Update `.env.example` files and record the map-key deferral boundary.

### Phase 4

- Run focused backend regression and frontend build.
- Update all progress artifacts.
- Mark T21 done only after docs and verification are both complete.

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

## Validation Rules

- Prefer focused backend tests first, then frontend build.
- Do not mark a phase complete without evidence from code or command output.
- Existing chunk-size warnings are not T21 blockers unless the user explicitly expands scope to build optimization.

## Completion Trigger

- When every checkbox in `docs/progress/MASTER.md` is complete, announce that T21 is finished.
- Before any cleanup or removal of generated docs or this skill, ask the user what to keep.
