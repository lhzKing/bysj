# T21 Config Externalization

- Task ID: T21
- Created At: 2026-04-14 10:40
- Status: DONE
- Source: `legacy project remediation task table`

## 1. Task Definition

After T25 was completed, the next non-UI logic task was **T21 Config Externalization**.
This task continues under the **Spec-Driven Develop** workflow. Old `docs/superpowers/*`
artifacts remain historical references only.

## 2. Goals

1. Move backend runtime settings to environment-overridable placeholders.
2. Remove CORS rule drift between `application.yml`, `CorsFilter`, and `WebMvcConfig`.
3. Move Vite dev server, HTTPS certificate, and proxy target settings to env-driven config.
4. Reserve a map-key env contract without editing frozen frontend component files.
5. Keep enough documentation so the next conversation can resume from `docs/progress/MASTER.md`.

## 3. Scope

### In Scope

- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/example/trace/config/CorsFilter.java`
- `backend/src/main/java/com/example/trace/config/WebMvcConfig.java`
- `backend/src/main/java/com/example/trace/config/CorsProperties.java`
- `backend/src/main/java/com/example/trace/config/CorsOriginMatcher.java`
- `backend/src/main/java/com/example/trace/TraceApplication.java`
- `backend/.env.example`
- `frontend/vite.config.js`
- `frontend/.env.example`
- `backend/src/test/java/com/example/trace/config/CorsOriginMatcherTest.java`

### Out of Scope

- Any frontend view/component file, including `frontend/src/features/trace/components/TraceRouteMap.vue`
- Map coordinate cleanup
- Actual map-key wiring inside the Vue component
- CI/CD or deployment-secret management
- UI or styling work

## 4. Problem Summary

1. `application.yml` contained hardcoded local development defaults for database, Redis, JWT, signature paths, and CORS.
2. `CorsFilter` maintained its own origin-allow logic independent from MVC config.
3. `vite.config.js` hardcoded host, port, cert paths, and `/api` proxy target.
4. `TraceRouteMap.vue` still contains a hardcoded AMap key, but that file is currently frozen.

## 5. Constraints

- Frontend UI/component files remain frozen unless the user explicitly re-authorizes edits.
- Do not continue the old superpowers workflow as the active process.
- Keep runnable defaults while still allowing env overrides.
- Update progress files and the root task table after each phase or subtask.

## 6. Acceptance Criteria

- Backend port, datasource, Redis, JWT, signature paths, logging level, and CORS are env-overridable.
- `CorsFilter` and `WebMvcConfig` share the same `CorsProperties` source.
- Vite dev host/port/https/cert/proxy settings are env-overridable.
- Backend and frontend `.env.example` files exist.
- `VITE_AMAP_KEY` is documented as a reserved contract.
- Real `TraceRouteMap.vue` map-key wiring is explicitly deferred to T23 while component files are frozen.
