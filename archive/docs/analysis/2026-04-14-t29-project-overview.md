# T29 Project Overview

## Focus Area

T29 is limited to low-risk cleanup of historical residue that is no longer referenced inside the repository:

- `TokenStore` still contained old static token API stubs that only throw `UnsupportedOperationException`.
- `TraceController` still exposed deprecated legacy paths alongside the newer RESTful trace endpoints.
- `LoginResponse` still kept an old constructor shape once callers were updated to explicitly provide permissions.
- `ApiResponse` still kept an unused deprecated one-arg `fail(String)` alias.

## Desired End State

- Unused compatibility stubs are removed.
- Old repository-internal call patterns are migrated to the explicit current shape.
- Current behavior for active endpoints and active response payloads remains unchanged.
