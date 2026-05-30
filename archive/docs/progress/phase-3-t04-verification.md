# Phase 3: Verification

- [x] Run focused trace tests.
- [x] Confirm there are no remaining references to `selectByTraceCodeForUpdate`.

## Notes

- `cd backend && mvn test "-Dtest=TraceServiceImplTest,TraceDemoDataServiceImplTest,TraceScanTransactionServiceTest,TraceScanRetryExecutorTest"` passed (10 tests).
- Source search confirmed there are no remaining references to `selectByTraceCodeForUpdate`.
