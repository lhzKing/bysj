# T22 Task Breakdown

## Execution Plan Table

| Phase | Focus | Tasks | Acceptance Criteria |
| --- | --- | --- | --- |
| Phase 1 | Scope audit | Confirm consumers, frozen-file boundary, and compatibility requirement | T22 scope is locked |
| Phase 2 | Region dictionary split | Extract `REGIONS` and related query helpers | Region dictionary is managed independently |
| Phase 3 | Node mapping split | Extract `NODE_REGION_MAP` and `getRegionByNode(...)`; keep compatibility barrel | Node presets are managed independently and old imports still work |
| Phase 4 | Verification & closeout | Add tests, run focused verification, sync docs and task table | Behavior is stable and resumable across conversations |

## Detailed Tasks

### Phase 1 - Scope audit
- [x] Audit current `regions.js` responsibilities
- [x] Confirm current consumers are frozen frontend components
- [x] Lock T22 to shared-data cleanup only

### Phase 2 - Region dictionary split
- [x] Add `catalog.js`
- [x] Extract `REGIONS`
- [x] Add region query helpers

### Phase 3 - Node mapping split
- [x] Add `node-region-map.js`
- [x] Add `lookup.js`
- [x] Convert `regions.js` into a compatibility barrel

### Phase 4 - Verification & closeout
- [x] Add `regions.test.js`
- [x] Run focused shared-data plus scan-flow contract verification
- [x] Run frontend build
- [x] Sync `MASTER.md`, phase files, task table, and project-level skill
