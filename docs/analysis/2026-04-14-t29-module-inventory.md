# T29 Module Inventory

| Module / File | Residue Type | Evidence | Action |
| --- | --- | --- | --- |
| `security/TokenStore.java` | Old static compatibility methods | Repository search showed no usages; methods only threw `UnsupportedOperationException` | Remove |
| `controller/TraceController.java` | Deprecated legacy endpoints | Repository search showed no frontend/backend usages; frontend trace API already uses `/api/traces` RESTful paths | Remove |
| `dto/LoginResponse.java` | Old constructor shape | Only two current call sites remained and can pass explicit empty permissions | Migrate callers and remove constructor |
| `common/ApiResponse.java` | Old deprecated alias | One-arg `fail(String)` had no repository usages after T28 | Remove |
| `controller/AuthController.java` | Historical constructor usage | Still used old `LoginResponse(token, username, role)` calls | Migrate to 4-arg constructor |
