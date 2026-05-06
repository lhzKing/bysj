# MASTER - T22 `Split regions.js data responsibility`

## Task

- Task ID: T22
- Task Name: Split `regions.js` data responsibility
- Status: DONE
- Summary: Split mixed frontend region dictionary and node preset mapping into dedicated modules while preserving the existing compatibility import entry.

## Source Documents

- Spec: [2026-04-14-t22-regions-data-split.md](../specs/2026-04-14-t22-regions-data-split.md)

### Analysis

- [2026-04-14-t22-project-overview.md](../analysis/2026-04-14-t22-project-overview.md)
- [2026-04-14-t22-module-inventory.md](../analysis/2026-04-14-t22-module-inventory.md)
- [2026-04-14-t22-risk-assessment.md](../analysis/2026-04-14-t22-risk-assessment.md)

### Plan

- [2026-04-14-t22-task-breakdown.md](../plan/2026-04-14-t22-task-breakdown.md)
- [2026-04-14-t22-dependency-graph.md](../plan/2026-04-14-t22-dependency-graph.md)
- [2026-04-14-t22-milestones.md](../plan/2026-04-14-t22-milestones.md)

## Execution Plan Table

| Phase | Focus | Status | Result |
| --- | --- | --- | --- |
| Phase 1 | Scope audit | DONE | Consumers, frozen-file boundary, and compatibility requirements locked |
| Phase 2 | Region dictionary split | DONE | `REGIONS` and region query helpers extracted into `catalog.js` |
| Phase 3 | Node mapping split | DONE | `NODE_REGION_MAP` and lookup logic extracted; `regions.js` reduced to compatibility barrel |
| Phase 4 | Verification & closeout | DONE | Focused tests and frontend build passed; docs and task table synced |

## Detailed Phase Files

- [x] Phase 1: Scope audit [details](./phase-1-t22-scope-audit.md)
- [x] Phase 2: Region dictionary split [details](./phase-2-t22-data-extraction.md)
- [x] Phase 3: Node mapping split & compatibility layer [details](./phase-3-t22-compatibility-layer.md)
- [x] Phase 4: Verification & closeout [details](./phase-4-t22-verification-closeout.md)

## Current Status

- Updated At: 2026-05-03 12:54
- State: Completed / Root review remediation continuing post-close
- Completed Work:
  - Audited current `regions.js` consumers and confirmed they are frozen frontend components
  - Extracted the province/city dictionary into `frontend/src/shared/data/regions/catalog.js`
  - Extracted the node preset map into `frontend/src/shared/data/regions/node-region-map.js`
  - Extracted `getRegionByNode(...)` into `frontend/src/shared/data/regions/lookup.js`
  - Kept `frontend/src/shared/data/regions.js` as the stable compatibility export entry
  - Added `frontend/src/shared/data/__tests__/regions.test.js`
  - Passed focused Vitest verification and frontend production build
  - Post-close note (2026-04-16): backend role-governance hotfix now blocks `ADMIN` from modifying `SUPER_ADMIN` and from delegating `user:*` / `role:*` management permissions to lower roles
  - Post-close note (2026-04-17): role management UI now mirrors the backend guardrail by hiding `SUPER_ADMIN` / same-level mutation actions from `ADMIN`, filtering `user:*` / `role:*` options for non-`SUPER_ADMIN`, and persisting `roleCode` / `permissions` in the user store; focused `RoleList.contract.test.js`, targeted `RoleServiceImplTest`, and frontend build all passed
  - Post-close note (2026-04-17 00:42): quick UI rollback restored the original role-management page presentation in `RoleList.vue` / `RoleTable.vue` while keeping all newly added permission-guard logic unchanged; focused frontend test and build passed again
  - Post-close note (2026-04-17 01:04): `RoleList.vue` was rebuilt directly on top of the historical page skeleton provided by the user so the old spacing/width/padding rhythm is back, while `Protected` row markers and the frontend role-governance guard logic remain intact
  - Post-close note (2026-05-02): R-P0-01 post-close hardening of the earlier T21 config line added dev/test/prod backend profiles, production JWT/database fail-fast checks, dev/test-only MyBatis stdout SQL logging, and placeholder-only backend `.env.example`; backend targeted (13 tests) and full Maven tests passed.
  - Post-close note (2026-05-02): R-P0-02 post-close hardening removed workspace RSA pem files, switched dev/test signature mode to in-memory key generation, required prod signature key paths to be externally mounted, added SignatureUtil/prod guard tests, and updated README guidance; backend targeted (18 tests) and full Maven tests passed.
  - Post-close note (2026-05-02): R-P0-02 continuation configured fixed external backup keys from D:/trace-runtime/keys for dev/test, added signature_key_id / signature_key_version schema and DTO metadata, made verification key-aware, exposed key id/version from public-key API, and passed targeted (24 tests) plus full Maven tests.
  - Post-close note (2026-05-02): R-P0-03 removed the full Authorization Header log, added a regression test proving raw Bearer token text is not logged, sanitized JWT/TokenStore exception logging to fixed reasons or exception types, normalized credential/session logs in auth/user services, reconfirmed prod/default configs do not enable MyBatis `StdOutImpl`, and passed targeted (16 tests) plus full Maven tests.
  - Post-close note (2026-05-02): R-P0-04 changed Redis-backed token blacklist failures to fail-closed semantics, added `TokenStoreException`, returned 503 when auth state storage is unavailable, exposed blacklist write failures from logout/refresh/password-change flows, and passed targeted (21 tests) plus full Maven tests.
  - Post-close note (2026-05-02): R-P0-05 changed map InfoWindow rendering to DOM API + `textContent`, added an XSS regression test for malicious node names, added backend length/safe-character validation for scan route fields, and passed focused frontend/backend tests, frontend build, and full Maven tests.
  - Post-close note (2026-05-02): R-P0-06 added profile-controlled demo-data admin guardrails (`TRACE_DEMO_DATA_ENABLED` default false, dev/test true), split admin endpoints to dedicated `trace:data:generate` / `trace:data:clear` permissions, required `confirm=DELETE_TRACE_DATA` for destructive cleanup, added audit logging and a `count<=500` limit, updated SQL seed/migration plus README/api-doc guidance, and passed targeted (8 tests) plus full Maven tests.
  - Post-close note (2026-05-02): R-P1-01 rewired the login-page self-service registration flow from `POST /api/users` to `POST /api/auth/register`, added `auth.register(username, password)`, removed hardcoded `roleId/status` from `Login.vue`, kept `/api/users` as the privileged management entry, added focused frontend registration/contract tests plus a backend `AuthController` register-default-role test, and passed targeted frontend (8 tests), targeted backend, and full Maven tests.
  - Post-close note (2026-05-02): R-P1-02 introduced `AuthService` / `AuthServiceImpl`, moved login/register/logout/refresh/me/change-password business logic out of `AuthController`, reduced the controller to HTTP binding + response wrapping only, replaced controller-heavy tests with controller delegation coverage plus dedicated `AuthServiceImplTest`, and passed targeted backend (24 tests) plus full Maven tests.
  - Post-close note (2026-05-02): R-P1-03 converged the scan permission model by defining `trace:scan` as the super scan permission, changing fine-grained `trace:inbound/outbound/transfer` inheritance to `trace:view` only, tightening `TraceController` action checks, narrowing scan-center route/navigation visibility to operational permissions, updating `ScanHub` button gating, refreshing seed/docs, and passing focused frontend tests, frontend build, targeted backend tests, and full Maven tests.
  - Post-close note (2026-05-02): R-P1-04 kept `remark` as a formal optional trace event audit field, added DTO/entity/mapper/schema/migration coverage, normalizes and validates remarks before persistence, includes non-blank remarks in log hash/signature payloads while preserving old empty-remark compatibility, exposes remarks in trace history/timeline, refreshed README/api-doc/Postman docs, and passed full Maven tests, focused frontend contract test, and frontend build; full `npm run test:run` still fails only in the pre-existing R-P1-08 useConfirm/MainLayout suites.
  - Post-close note (2026-05-02): R-P1-05 replaced lenient eventTime parsing with strict ISO-8601 local datetime validation, preserved blank eventTime default-to-now behavior, rejects invalid nonblank eventTime with PARAM_ERROR/400 before snapshot lookup/persistence, changed ScanFlowDialog submission to `YYYY-MM-DDTHH:mm:ss`, refreshed API/Postman/README docs, and passed targeted backend/frontend tests, full Maven tests, and frontend build.
  - Post-close note (2026-05-03): R-P1-06 added explicit part-deletion referential integrity protection: delete and batch delete now check trace_snapshot / trace_lifecycle_log SPU references first, return CONFLICT/409 for referenced parts without partial deletion, added mapper lookups and PartServiceImpl tests, added init_schema foreign keys/indexes plus migrate_v6_part_reference_constraints.sql, refreshed API/Postman/README docs, and passed targeted backend tests plus full Maven tests.
  - Post-close note (2026-05-03): R-P1-07 capped production trace-code assignment at `quantity<=500` with DTO Bean Validation plus service-layer fail-safe validation before mapper access, hardened demo-data generation so `TRACE_DEMO_DATA_MAX_GENERATE_COUNT` can only lower the 500 hard cap, refreshed docs/env guidance, and passed targeted quantity/count tests plus full Maven tests.
  - Post-close note (2026-05-03): R-P1-08 restored the frontend test suite to green by rewriting `useConfirm` tests for the current local composable, updating MainLayout tests for the floating nav/mobile drawer shell with stable test anchors, and breaking the request/router/store/auth API cycle by replacing the direct router import in `request.js` with an injected unauthorized handler registered from the router; targeted frontend tests, full `npm run test:run`, and frontend build all passed.
  - Post-close note (2026-05-03): R-P1-09 synchronized critical README/API/Postman/backend README documentation with the current implementation: self-service registration uses `POST /api/auth/register`, current user uses `GET /api/auth/me`, refresh uses `remember_me`, auth responses document `permissions`, old `/api/trace/*` paths are documented only as removed legacy routes, profile/JWT/RSA key metadata/Redis fail-closed/migration guidance was refreshed, and the frontend Axios sample now matches `/api` baseURL plus injected 401 handling; focused documentation drift searches passed.
  - Post-close note (2026-05-03): R-P2-01 converged backend CORS handling to a single high-priority `CorsFilter` write point, removed MVC `addCorsMappings` / `CorsRegistry` from `WebMvcConfig`, kept CORS rules on externalized `CorsProperties` plus `CorsOriginMatcher`, preserved `Authorization` exposure, added `CorsFilterTest` coverage for allowed origins, wildcard OPTIONS short-circuiting, and rejected origins, refreshed README/backend README, and passed focused CORS tests plus full Maven tests.
  - Post-close note (2026-05-03): R-P2-02 centralized role hierarchy and governance rules into `RolePolicy`, removed duplicated role priority/system-role/protected-permission hardcoding from `UserServiceImpl` and `RoleServiceImpl`, locked the policy with `RolePolicyTest`, updated service tests and README/backend README directory notes, and passed focused role-policy tests plus full Maven tests.
  - Post-close note (2026-05-03): R-P2-03 unified user status validation by adding 0/1 Bean Validation constraints to create/update DTOs, extracting service-layer `validateStatus` / `validateRequiredStatus`, applying the guard before create/update/toggle persistence, adding service and DTO regression tests for invalid status, and passing focused tests plus full Maven tests.
- Next:
  - Post-close note (2026-05-03): R-P2-04 documented the Token storage decision, kept the current `Authorization` Header + `localStorage` model behind a new `frontend/src/core/auth/authStorage.js` adapter, shortened default JWT lifetimes to 2h / remember-login 1d, added a frontend CSP meta baseline plus production header guidance, replaced the frontend env example map key with a placeholder, refreshed README/API/Postman guidance, and passed focused/full frontend tests, frontend build, focused backend tests, and full Maven tests.
  - Post-close note (2026-05-03): R-P2-05 added root/frontend ignore rules for runtime diagnostics and generated artifacts, non-destructively archived 21 files (219.27 MiB) from the root/frontend/backend source directories into runtime_state/repo-hygiene/2026-05-03 with an archive manifest, documented the inventory and restore path in docs/analysis/2026-05-03-runtime-artifacts-inventory.md, and verified the original source locations no longer contain the targeted artifacts or root files larger than 10 MiB.
  - Post-close note (2026-05-03): R-P2-06 was reopened per user request and expanded from frontend JSDoc cleanup to full-project text encoding governance; added `tools/check_text_encoding.py` plus `.editorconfig`, converted legacy GB18030 text files to UTF-8, repaired backend/frontend/doc/Postman/runtime text mojibake, and passed both default and `--include-runtime` encoding scans plus full frontend tests, frontend build, and full Maven tests.
  - Post-close note (2026-05-03): R-P2-07 replaced the template `frontend/README.md` with a project-specific frontend guide covering startup, env vars, HTTPS certificates, `/api` proxying, npm scripts, tests, build/preview, map-key handling, route permissions, and troubleshooting; default and include-runtime encoding scans, full frontend tests, and frontend build passed.
  - Post-close note (2026-05-03): R-P2-08 added `WebMvcSecurityPathContractTest` to lock the real `WebMvcConfig` login/permission interceptor path contract and order, documented the startup/exception coverage map in `docs/analysis/2026-05-03-rp208-startup-and-exception-coverage.md`, marked the remaining cross-task checklist items complete, and passed encoding scans, focused/backend full Maven tests, and full frontend tests.
  - Root review remediation table is fully closed: 23 tasks DONE, 0 TODO.
