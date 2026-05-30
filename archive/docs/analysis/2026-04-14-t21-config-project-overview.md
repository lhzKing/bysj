# T21 Project Overview

## Background

- Workspace: `d:/bysj`
- Current remediation line: keep working from `legacy project remediation task table`
- Previous completed task: T25 `PermissionService` split
- Current task: T21 config externalization

## Runtime Entry Points Relevant to T21

| Area | File | Purpose |
| --- | --- | --- |
| Backend bootstrap | `backend/src/main/resources/application.yml` | Main Spring runtime configuration |
| Backend CORS filter | `backend/src/main/java/com/example/trace/config/CorsFilter.java` | Servlet-level CORS headers |
| Backend MVC config | `backend/src/main/java/com/example/trace/config/WebMvcConfig.java` | MVC CORS + interceptors |
| Backend config binding | `backend/src/main/java/com/example/trace/TraceApplication.java` | Enables `@ConfigurationProperties` |
| Frontend dev config | `frontend/vite.config.js` | Dev server, HTTPS, proxy |
| Frontend map component | `frontend/src/features/trace/components/TraceRouteMap.vue` | Contains hardcoded AMap key; file is frozen |

## Tech Stack

| Directory | Stack | Relation to T21 |
| --- | --- | --- |
| `backend/` | Spring Boot 3.2.2, MyBatis-Plus, MySQL, Redis, JWT | Main implementation area |
| `frontend/` | Vue 3, Vite 5, Vitest | Only build/dev config files can change |
| `docs/` | Historical superpowers docs + current spec-driven docs | Resume point for future conversations |

## Config Surface Inventory

### Backend

- `server.port`
- `spring.datasource.*`
- `spring.data.redis.*`
- `jwt.*`
- `cors.*`
- `trace.signature.*`
- `logging.level.com.example.trace.security`

### Frontend

- `server.host`
- `server.port`
- `server.https`
- `server.https.key`
- `server.https.cert`
- `server.proxy['/api'].target`

## Boundary Decision

1. Backend runtime config and Vite dev config are in scope for T21.
2. Actual map-key wiring requires editing a frozen `.vue` component, so it is not executable in this session.
3. T23 already covers map key / coordinate cleanup, so the final component-level work should land there.
4. T21 closes on config governance plus explicit deferral notes.
