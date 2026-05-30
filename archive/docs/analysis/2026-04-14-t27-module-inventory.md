# T27 Module Inventory

| Module / File | Responsibility | Current Issue | Planned Action |
| --- | --- | --- | --- |
| `common/BizCode.java` | Business error-code registry | No centralized HTTP mapping helper | Add status inference helper |
| `common/BizException.java` | Business exception carrier | Local mapping coverage incomplete | Delegate to `BizCode` and keep override path |
| `common/GlobalExceptionHandler.java` | Final API error envelope | Still mixes hardcoded statuses for framework exceptions | Keep scope narrow; only align with centralized mapping where natural |
| `controller/AuthController.java` | Login/register/password endpoints | Many repeated explicit status constructors | Refactor obvious deterministic cases; keep `old password wrong` explicit |
| `controller/TraceController.java` | Trace API entrypoint | Repeats forbidden/not-found statuses | Refactor to inferred mapping where unambiguous |
| `service/impl/support/TraceCodeAssignmentService.java` | Produce assign validation | Repeated `PARAM_ERROR, 400` calls | Refactor to inferred mapping |
| `service/impl/support/TraceScanRetryExecutor.java` | Retry / conflict handling | Repeated conflict / server-error statuses | Refactor to inferred mapping |
| `service/impl/support/TraceScanTransactionService.java` | Scan transaction rules | Repeated not-found / bad-request statuses | Refactor to inferred mapping |
| `util/SignatureUtil.java` | RSA sign/verify utility | Raw `RuntimeException` on init/sign failure | Replace with business exception path |
| `backend tests` | Regression safety net | Missing direct mapping lock test | Add focused unit test |
