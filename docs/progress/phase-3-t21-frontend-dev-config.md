# Phase 3: Frontend Dev Config

## Goal

Externalize frontend dev-server, certificate, and proxy config without touching frozen UI/component files.

## Parallel Lane

- Lane 1 only (sequential, Merge Risk = Low)

## Tasks

- [x] T21-3.1 Externalize Vite dev server / HTTPS / cert / proxy config; AC: `vite.config.js` reads `VITE_DEV_*` and `VITE_API_PROXY_TARGET` through `loadEnv`.
- [x] T21-3.2 Add env templates and document the map-key deferral; AC: `frontend/.env.example` reserves `VITE_AMAP_KEY` and docs point real wiring to T23.

## Notes

- 2026-04-14 10:28: `vite.config.js` was converted to `defineConfig(({ mode }) => loadEnv(...))`.
- 2026-04-14 10:28: backward-compatible defaults were preserved for local development.
- 2026-04-14 10:29: no `.vue` UI/component file was changed.
