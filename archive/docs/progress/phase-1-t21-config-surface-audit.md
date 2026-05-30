# Phase 1: Config Surface Audit

## Goal

Identify hardcoded runtime configuration surfaces and lock the scope boundary before implementation.

## Parallel Lane

- Lane 1 only (sequential, Merge Risk = Low)

## Tasks

- [x] T21-1.1 Audit backend/frontend config surfaces; AC: locate `application.yml`, `CorsFilter`, `WebMvcConfig`, `vite.config.js`, and `TraceRouteMap.vue`.
- [x] T21-1.2 Confirm scope boundary; AC: component files remain frozen and real map-key wiring is deferred to T23.

## Notes

- 2026-04-14 10:20: confirmed there was no current in-progress task and T21 was the next recommended non-UI task.
- 2026-04-14 10:22: identified backend runtime config, duplicate CORS logic, and Vite dev/proxy hardcoding.
- 2026-04-14 10:23: confirmed `TraceRouteMap.vue` is a frozen component file and is read-only in this task.
