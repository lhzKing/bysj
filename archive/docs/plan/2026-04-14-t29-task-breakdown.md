# T29 Task Breakdown

## Execution Plan Table

| Phase | Focus | Tasks | Acceptance Criteria |
| --- | --- | --- | --- |
| Phase 1 | Residue audit | Identify zero-reference compatibility stubs, constructors, and old interfaces | Cleanup candidates and boundary are documented |
| Phase 2 | Compatibility stub cleanup | Remove unused helper/compatibility stubs | Zero-reference compatibility code is deleted without affecting active flows |
| Phase 3 | Old interface / constructor cleanup | Remove old trace endpoints and migrate old constructor usage | Active code paths use only current interfaces |
| Phase 4 | Verification & closeout | Run backend verification, run residue searches, sync docs/task table | Tests pass and residue search confirms intended cleanup |

## Detailed Tasks

### Phase 1 - Residue audit
- [x] Audit deprecated/legacy markers and zero-reference candidates
- [x] Confirm frontend trace API already uses new RESTful endpoints
- [x] Lock T29 to low-risk zero-reference cleanup

### Phase 2 - Compatibility stub cleanup
- [x] Remove unused `TokenStore` static compatibility methods
- [x] Remove unused `ApiResponse.fail(String)` alias
- [x] Keep currently used compatibility aliases out of scope

### Phase 3 - Old interface / constructor cleanup
- [x] Migrate `LoginResponse` callers to the explicit 4-arg constructor
- [x] Remove the old `LoginResponse` 3-arg constructor
- [x] Remove unused deprecated legacy trace endpoints

### Phase 4 - Verification & closeout
- [x] Run backend tests
- [x] Run focused residue searches
- [x] Update `MASTER.md`, phase files, and `项目整改执行任务表.md`
