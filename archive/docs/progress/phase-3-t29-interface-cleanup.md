# Phase 3: Old Interface Cleanup

- [x] Migrate `LoginResponse` callers to the explicit 4-arg constructor.
- [x] Remove the old `LoginResponse` 3-arg constructor.
- [x] Remove unused deprecated legacy trace endpoints.

## Notes

- `AuthController.register(...)` and `AuthController.refreshToken(...)` now pass explicit empty permissions lists.
- Removed the old `LoginResponse(String token, String username, String role)` constructor after migrating its only remaining call sites.
- Removed `TraceController.produceAssignLegacy(...)`, `scanLegacy(...)`, and `detailLegacy(...)` after confirming zero repository usages.
