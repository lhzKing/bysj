# T28 Task Breakdown

## Execution Plan Table

| Phase | Focus | Tasks | Acceptance Criteria |
| --- | --- | --- | --- |
| Phase 1 | Factory audit | Inventory duplicated `ApiResponse` factories and main usage hotspots | Canonical convergence scope is documented |
| Phase 2 | ApiResponse factory convergence | Extract canonical success/failure factory paths and keep aliases delegating | `ApiResponse` no longer hardcodes the same literals across many public factories |
| Phase 3 | Usage convergence | Update obvious controller / handler usages to the canonical factories | Mainline success/failure usage is more uniform without semantic drift |
| Phase 4 | Verification & closeout | Add focused tests, run backend verification, sync docs/task table | Tests pass and documents reflect final state |

## Detailed Tasks

### Phase 1 - Factory audit
- [x] Review `ApiResponse` duplicated success/error factories
- [x] Review controller / exception-handler usage hotspots
- [x] Lock low-risk T28 scope

### Phase 2 - ApiResponse factory convergence
- [x] Extract canonical success factory path
- [x] Extract canonical failure factory path
- [x] Keep explicit-status override path and low-risk aliases

### Phase 3 - Usage convergence
- [x] Replace obvious `ApiResponse.ok(...)` usage with canonical success factory
- [x] Reuse canonical failure factory where natural
- [x] Avoid payload-contract changes

### Phase 4 - Verification & closeout
- [x] Add focused response-factory tests
- [x] Run backend verification
- [x] Update `MASTER.md`, phase files, and `项目整改执行任务表.md`
