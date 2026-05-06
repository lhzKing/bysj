# T21 Module Inventory

## Backend Modules

| File | Responsibility | Complexity | T21 Action |
| --- | --- | --- | --- |
| `backend/src/main/resources/application.yml` | Spring runtime defaults | M | Convert key entries to env placeholders |
| `backend/src/main/java/com/example/trace/config/CorsFilter.java` | Servlet-level CORS handling | M | Remove hardcoded origin decisions; use shared config |
| `backend/src/main/java/com/example/trace/config/WebMvcConfig.java` | MVC interceptors + CORS | M | Read from `CorsProperties` |
| `backend/src/main/java/com/example/trace/config/CorsProperties.java` | Bound CORS config object | S | New |
| `backend/src/main/java/com/example/trace/config/CorsOriginMatcher.java` | Exact + wildcard origin matcher | S | New |
| `backend/src/main/java/com/example/trace/TraceApplication.java` | Spring bootstrap | S | Enable config properties binding |
| `backend/.env.example` | Backend env template | S | New |

## Frontend Modules

| File | Responsibility | Complexity | T21 Action |
| --- | --- | --- | --- |
| `frontend/vite.config.js` | Vite dev/build config | M | Switch to `loadEnv` |
| `frontend/.env.example` | Frontend env template | S | New |
| `frontend/src/features/trace/components/TraceRouteMap.vue` | Map component | M | Read-only risk item in this task |

## Test Assets

| File | Coverage | Why It Matters |
| --- | --- | --- |
| `backend/src/test/java/com/example/trace/config/CorsOriginMatcherTest.java` | Exact + wildcard CORS matching | Locks new matcher behavior |
| `backend/src/test/java/com/example/trace/controller/AuthControllerTest.java` | Spring auth flow | Verifies config changes do not break common paths |
| `backend/src/test/java/com/example/trace/security/LoginInterceptorTest.java` | Login interceptor behavior | Regression coverage |
| `backend/src/test/java/com/example/trace/security/PermissionInterceptorTest.java` | Permission interceptor behavior | Regression coverage |
| `backend/src/test/java/com/example/trace/security/PermissionServiceTest.java` | Permission service behavior | Protects prior T25 work |

## Conclusion

- T21 touches a narrow set of backend config files plus Vite config and env templates.
- Merge risk is low.
- The only meaningful incomplete surface is the frozen map component, which is explicitly deferred.
