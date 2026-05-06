# T21 Task Breakdown

## Goal

Externalize backend runtime config and frontend dev config without touching frozen frontend UI/component files, while documenting the deferred map-key wiring boundary.

## Phase 1: Config Surface Audit

### Lane 1 (Sequential, Merge Risk: Low)

#### T21-1.1 Audit hardcoded config surfaces

- Priority: P1
- Effort: S
- Depends on: none
- Description: identify runtime config entry points in backend and frontend.
- Acceptance:
  - locate `application.yml`, `CorsFilter`, `WebMvcConfig`, `vite.config.js`, and the map-key holder
  - distinguish runtime config from frozen component logic

#### T21-1.2 Confirm boundary and deferrals

- Priority: P1
- Effort: S
- Depends on: T21-1.1
- Description: lock the task scope before writing code.
- Acceptance:
  - frontend component files remain untouched
  - real map-key wiring is deferred to T23 or future explicit authorization

## Phase 2: Backend Runtime Config Externalization

### Lane 1 (Sequential, Merge Risk: Medium)

#### T21-2.1 Externalize backend runtime settings

- Priority: P1
- Effort: M
- Depends on: T21-1.2
- Description: move port, datasource, Redis, JWT, signature paths, and logging level to env placeholders.
- Acceptance:
  - key `application.yml` values are env-overridable
  - `backend/.env.example` exists

#### T21-2.2 Unify CORS config sources

- Priority: P1
- Effort: M
- Depends on: T21-2.1
- Description: add shared CORS properties and matcher for both filter and MVC config.
- Acceptance:
  - `CorsFilter` and `WebMvcConfig` read the same CORS config object
  - exact and wildcard origin matching is covered by tests

## Phase 3: Frontend Dev Config Externalization

### Lane 1 (Sequential, Merge Risk: Low)

#### T21-3.1 Externalize Vite dev/proxy/cert settings

- Priority: P1
- Effort: S
- Depends on: T21-2.2
- Description: switch Vite config to `loadEnv`.
- Acceptance:
  - no hardcoded dev host/port/proxy target remain in active Vite config logic
  - `frontend/.env.example` exists

#### T21-3.2 Reserve the map-key env contract

- Priority: P1
- Effort: S
- Depends on: T21-3.1
- Description: document the env contract without editing the frozen component file.
- Acceptance:
  - `VITE_AMAP_KEY` is present in docs/templates
  - T23 is named as the real component-level cleanup task

## Phase 4: Verification & Closeout

### Lane 1 (Sequential, Merge Risk: Low)

#### T21-4.1 Run focused regression validation

- Priority: P0
- Effort: S
- Depends on: T21-3.2
- Description: run backend focused tests and frontend build.
- Acceptance:
  - targeted backend tests pass
  - frontend build passes

#### T21-4.2 Update docs and task table

- Priority: P0
- Effort: S
- Depends on: T21-4.1
- Description: sync MASTER, phase files, sub-skill, and root task table.
- Acceptance:
  - `docs/progress/MASTER.md` is the next resume point
  - the root task table marks T21 complete and points to the next task

## Parallel Execution Note

- No user authorization for sub-agent parallel implementation was given, so actual execution remained sequential.
- The phase/lane structure is still documented for future controlled delegation.
