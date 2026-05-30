# T21 Risk Assessment

## Risk 1: CORS Source Drift

- Symptom: `application.yml`, `CorsFilter`, and `WebMvcConfig` could diverge.
- Impact: Preflight and actual request behavior may differ.
- Mitigation: Share `CorsProperties` and `CorsOriginMatcher` across both entry points.

## Risk 2: Dev Defaults Still Exist

- Symptom: Env placeholders keep local fallback defaults.
- Impact: A deployment that forgets env injection could still fall back to local values.
- Mitigation: Add `.env.example` templates and document that deployment should provide real values.

## Risk 3: Frozen Map Component Prevents Full Key Externalization

- Symptom: `TraceRouteMap.vue` directly builds the AMap script URL.
- Impact: T21 cannot finish component-level key wiring without violating the current freeze.
- Mitigation: Reserve `VITE_AMAP_KEY` and explicitly defer actual wiring to T23.

## Risk 4: HTTPS Cert Path Depends on Local Files

- Symptom: Dev HTTPS still needs actual cert files.
- Impact: New environments may fail to start Vite with HTTPS enabled.
- Mitigation: Keep `VITE_DEV_HTTPS` plus env-driven cert paths so HTTPS can be disabled or redirected.

## Risk 5: Existing Frontend Build Chunk Warning

- Symptom: `npm run build` reports large chunks.
- Impact: Can be mistaken as a regression from T21.
- Mitigation: Record it as pre-existing and non-blocking for this task.
