# T22 Module Inventory

| Module | Responsibility | Consumers | T22 Action |
| --- | --- | --- | --- |
| `frontend/src/shared/data/regions.js` | Historical mixed entrypoint | Trace form components, contract tests | Reduce to compatibility barrel |
| `frontend/src/shared/data/regions/catalog.js` | Province/city dictionary | Re-exported through compatibility barrel | New |
| `frontend/src/shared/data/regions/node-region-map.js` | Node preset mapping | Re-exported through compatibility barrel | New |
| `frontend/src/shared/data/regions/lookup.js` | Lookup behavior | Re-exported through compatibility barrel | New |
| `frontend/src/shared/data/__tests__/regions.test.js` | Shared-data contract lock | Vitest | New |
