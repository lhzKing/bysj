# Phase 1: Residue Audit

- [x] Audit deprecated/legacy markers and zero-reference candidates.
- [x] Confirm frontend trace API already uses new RESTful endpoints.
- [x] Lock T29 to low-risk zero-reference cleanup.

## Notes

- Repository search showed no usages of the old static compatibility methods in `TokenStore`.
- Repository search showed no usages of the deprecated legacy trace paths; frontend trace API already calls `/api/traces`, `/api/traces/{traceCode}/events`, and `/api/traces/{traceCode}`.
- `AuthController` still had two call sites using the old 3-arg `LoginResponse` constructor, so those callers were migrated in the same change set.
