# T27 Task Breakdown

## Execution Plan Table

| Phase | Focus | Tasks | Acceptance Criteria |
| --- | --- | --- | --- |
| Phase 1 | Status mapping audit | Inventory current codes, explicit overrides, and raw runtime paths | Mapping gaps and intentional overrides are documented |
| Phase 2 | Central mapping convergence | Centralize `BizCode -> HTTP status`, update `BizException` to delegate | Default exception construction covers all current codes |
| Phase 3 | Call-site convergence | Refactor obvious explicit constructors and raw runtime throws | Repeated status literals are reduced without broad behavior drift |
| Phase 4 | Verification & closeout | Add/adjust tests, run backend verification, sync docs/task table | Tests pass and cross-conversation docs reflect final state |

## Detailed Tasks

### Phase 1 - Status mapping audit
- [x] Review `BizCode`, `BizException`, `GlobalExceptionHandler`
- [x] Review repeated explicit `BizException(code, status, message)` hotspots
- [x] Lock low-risk scope and exceptions to keep explicit

### Phase 2 - Central mapping convergence
- [x] Add centralized HTTP status inference to `BizCode`
- [x] Make `BizException` delegate to centralized mapping
- [x] Keep explicit override constructor available

### Phase 3 - Call-site convergence
- [x] Refactor deterministic call sites to inferred mapping
- [x] Preserve intentional override cases
- [x] Replace `SignatureUtil` raw runtime failures with business exceptions

### Phase 4 - Verification & closeout
- [x] Add focused unit tests for mapping / override behavior
- [x] Run backend tests
- [x] Update `MASTER.md`, phase files, and `项目整改执行任务表.md`
