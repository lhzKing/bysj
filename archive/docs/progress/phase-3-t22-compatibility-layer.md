# Phase 3: Node Mapping Split & Compatibility Layer

- [x] Extract `NODE_REGION_MAP` into `frontend/src/shared/data/regions/node-region-map.js`.
- [x] Move `getRegionByNode(...)` into `frontend/src/shared/data/regions/lookup.js`.
- [x] Convert `frontend/src/shared/data/regions.js` into a compatibility barrel.

## Notes

- `getRegionByNode(...)` keeps the original exact-match-first behavior plus the existing two-character fuzzy fallback.
- The compatibility barrel still exports `REGIONS`, `NODE_REGION_MAP`, and `getRegionByNode`, and now also exposes region query helpers.
