# Phase 2: Compatibility Shim Cleanup

- [x] Remove unused `TokenStore` compatibility statics.
- [x] Remove old `LoginResponse` compatibility constructor after updating callers.
- [x] Remove unused `ApiResponse.fail(String)` shim.

## Notes

- `AuthController` register / refresh now explicitly pass an empty permissions list when constructing `LoginResponse`.
- `TokenStore` old static methods were confirmed unused and only threw `UnsupportedOperationException`.
- `ApiResponse.fail(String)` had no remaining callers after T28.
