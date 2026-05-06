# Phase 2: Compatibility Stub Cleanup

- [x] Remove unused `TokenStore` static compatibility methods.
- [x] Remove unused `ApiResponse.fail(String)` alias.
- [x] Keep currently used compatibility aliases out of scope.

## Notes

- Removed the historical static `TokenStore.generateToken/isValid/getUser/remove` stubs because they had zero repository usages and only threw `UnsupportedOperationException`.
- Removed the unused deprecated `ApiResponse.fail(String)` alias; current code now uses explicit `fail(code, ...)` / convenience factories.
- Kept `ApiResponse.ok(...)` out of scope because T28 intentionally preserved it as a compatibility alias and tests still lock that behavior.
