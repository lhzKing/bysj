# T22 Split `regions.js` Data Responsibility

- Task ID: T22
- Created At: 2026-04-14 17:48
- Status: DONE
- Source: root task table markdown file

## Task Definition

Split the mixed province/city dictionary and node-to-region preset map that previously lived together in `frontend/src/shared/data/regions.js`, while keeping the existing compatibility import entry stable for frozen frontend components.

## Goals

1. Move the region dictionary into a dedicated module.
2. Move the node preset map into a dedicated module.
3. Keep `@/shared/data/regions` working as the compatibility entry.
4. Preserve current `getRegionByNode(...)` behavior.
5. Avoid editing frontend view/component files.

## Scope

- `frontend/src/shared/data/regions.js`
- `frontend/src/shared/data/regions/catalog.js`
- `frontend/src/shared/data/regions/node-region-map.js`
- `frontend/src/shared/data/regions/lookup.js`
- `frontend/src/shared/data/__tests__/regions.test.js`

## Final Result

- Extracted the region dictionary into `catalog.js`.
- Extracted the node preset map into `node-region-map.js`.
- Extracted lookup behavior into `lookup.js`.
- Reduced `regions.js` to a compatibility barrel.
- Added focused shared-data coverage and passed the existing scan-flow contract test plus frontend build.
