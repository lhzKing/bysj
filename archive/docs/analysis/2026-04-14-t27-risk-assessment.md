# T27 Risk Assessment

## Primary Risks

1. **HTTP status drift**
   - Changing default mappings can alter API behavior.
   - Mitigation: only change mappings where semantics are clear; keep context-specific overrides explicit.

2. **Frontend auth handling coupling**
   - The frontend treats `401` specially.
   - Mitigation: preserve `PASSWORD_ERROR -> 401` as the default for login-style failures; keep non-login old-password validation as an explicit `400` override.

3. **Task scope creep into T28**
   - Response factory cleanup is adjacent but separate.
   - Mitigation: do not redesign `ApiResponse`; only keep existing error envelope usage working.

4. **Hidden exception paths**
   - Generic runtime exceptions can bypass intended business semantics.
   - Mitigation: convert only obvious backend business-path raw runtime failures in `SignatureUtil`.

## Mapping Decisions for This Task

- Keep `PASSWORD_ERROR` default as `401`, but preserve explicit `400` for `changePassword` old-password mismatch.
- Treat `USER_EXISTS` as `409 Conflict` to align with duplicate-resource semantics.
- Add coverage for `BAD_REQUEST`, `CONCURRENT_CONFLICT`, `TRACE_ALREADY_EXISTS`, `INVALID_ACTION_TYPE`, `CORRECTION_TARGET_NOT_FOUND`, and `DASHBOARD_QUERY_ERROR`.
