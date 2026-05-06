# Phase 2: Backend Runtime Config

## Goal

Externalize backend runtime parameters and unify the CORS source of truth.

## Parallel Lane

- Lane 1 only (sequential, Merge Risk = Medium)

## Tasks

- [x] T21-2.1 Externalize port, datasource, Redis, JWT, signature paths, and logging level; AC: `application.yml` is env-overridable and `backend/.env.example` exists.
- [x] T21-2.2 Unify CORS config between filter and MVC; AC: shared `CorsProperties` / `CorsOriginMatcher` plus test coverage for exact and wildcard origin matching.

## Notes

- 2026-04-14 10:26: added `CorsProperties` and `CorsOriginMatcher`.
- 2026-04-14 10:27: enabled `@EnableConfigurationProperties(CorsProperties.class)` in `TraceApplication`.
- 2026-04-14 10:39: tightened `CorsFilter` so requests without `Origin` are no longer logged as disallowed origins.
- 2026-05-02: R-P0-01 post-close hardening split `application-dev.yml` / `application-test.yml` / `application-prod.yml`, moved MyBatis stdout SQL logging to dev/test only, and added production fail-fast checks for explicit JWT secret and non-default database credentials.
- 2026-05-02: R-P0-02 post-close hardening removed default workspace RSA key paths from dev/test config, switched dev/test signature keys to in-memory auto-generation, and required prod signature key paths to come from external deployment injection.
- 2026-05-02: R-P0-02 continuation configured dev/test fixed external RSA keys at D:/trace-runtime/keys, added signature key id/version config, and added trace log schema metadata for key-aware verification.
