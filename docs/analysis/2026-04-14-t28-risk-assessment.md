# T28 Risk Assessment

## Primary Risks

1. **Payload drift**
   - Any change to `code/status/message/data` defaults could break frontend assumptions.
   - Mitigation: keep payload shape and default success message unchanged.

2. **Created vs OK semantics**
   - Some endpoints correctly return 201 through `created(...)`.
   - Mitigation: keep `created(...)` unchanged in semantics and do not collapse it into 200 success.

3. **Explicit exception overrides**
   - T27 introduced intentional override cases such as `PASSWORD_ERROR -> 400` for old-password mismatch.
   - Mitigation: preserve `fail(code, status, message)` for explicit-status paths and do not infer where an override is already intentional.

4. **Over-scoping into T29 cleanup**
   - Removing all aliases could become a broad cleanup.
   - Mitigation: keep low-risk aliases compatible; only standardize obvious mainline usage.
