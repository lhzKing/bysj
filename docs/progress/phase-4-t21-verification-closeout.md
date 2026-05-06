# Phase 4: Verification & Closeout

## Goal

Finish validation, progress sync, and task-table updates so the next conversation can resume cleanly.

## Parallel Lane

- Lane 1 only (sequential, Merge Risk = Low)

## Tasks

- [x] T21-4.1 Run focused regression validation; AC: backend tests pass and frontend build passes.
- [x] T21-4.2 Sync docs and task table; AC: MASTER, phase files, sub-skill, and root task table are updated.

## Notes

- 2026-04-14 11:10: under an explicit one-off frontend authorization, `TraceRouteMap.vue` was switched from the hardcoded AMap key to `import.meta.env.VITE_AMAP_KEY`; `cd frontend && npm run build` passed again.
- 2026-04-14 11:00: `cd backend && mvn test "-Dtest=CorsOriginMatcherTest,AuthControllerTest,LoginInterceptorTest,PermissionInterceptorTest,PermissionServiceTest"` passed (14 tests).
- 2026-04-14 10:29: `cd frontend && npm run build` passed; the existing chunk warning remains non-blocking.
- 2026-04-14 10:40: `docs/progress/MASTER.md` now points to T21 and the next recommended task is T04.
- 2026-05-02: R-P0-01 verification passed with `mvn test -Dtest=ProdProfileConfigGuardTest,AuthControllerTest,LoginInterceptorTest,CorsOriginMatcherTest` (13 tests) and full `mvn test -q`.
- 2026-05-02: R-P0-02 verification passed with `mvn test -Dtest=SignatureUtilTest,ProdProfileConfigGuardTest,TraceChainVerifyServiceTest,TraceServiceImplTest` (18 tests) and full `mvn test -q`; `backend/keys/*.pem` no longer exists in the workspace.
- 2026-05-02: R-P0-02 continuation verification passed with `mvn test -Dtest=SignatureUtilTest,ProdProfileConfigGuardTest,TraceChainVerifyServiceTest,TraceServiceImplTest,TraceScanTransactionServiceTest,TraceDemoDataServiceImplTest` (24 tests) and full `mvn test -q`; external backup keys exist under `D:/trace-runtime/keys` and `backend` still has no `*.pem` files.
