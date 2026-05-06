# T28 Module Inventory

| Module / File | Responsibility | Current Issue | Planned Action |
| --- | --- | --- | --- |
| `common/ApiResponse.java` | Unified response envelope | Factory methods overlap and duplicate literals | Extract canonical success/failure builders and make aliases delegate |
| `common/GlobalExceptionHandler.java` | Framework / business exception response bridge | Uses explicit `fail(code,status,message)` directly for framework-path errors | Keep explicit override path for `BizException`; optionally reuse canonical inferred failure path elsewhere |
| `controller/AuthController.java` | Auth success responses | Uses `ok(...)` for standard 200 success | Switch to canonical success factory |
| `controller/TraceController.java` | Trace success responses | Uses `ok(...)` for standard 200 success | Switch to canonical success factory |
| `controller/DashboardController.java` | Dashboard success responses | Uses `ok(...)` for standard 200 success | Switch to canonical success factory |
| `backend tests` | Regression safety net | No direct ApiResponse factory lock tests | Add focused unit test |
