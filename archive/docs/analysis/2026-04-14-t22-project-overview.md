# T22 Project Overview

## Context

T22 is a frontend shared-data cleanup task. It does not change backend code and does not modify frozen frontend view/component files.

## Current State

Before T22, `frontend/src/shared/data/regions.js` mixed three responsibilities:

- the province/city dictionary `REGIONS`
- the node preset map `NODE_REGION_MAP`
- the lookup helper `getRegionByNode(...)`

Current direct consumers remain frozen trace form components:

- `InboundForm.vue`
- `OutboundForm.vue`
- `TransferForm.vue`

The existing `ScanFlowDialog.contract.test.js` also depends on the `REGIONS` export contract.

## Boundary

- Shared data files and related tests are in scope.
- Frontend component import paths must stay stable.
- No UI/component refactor is allowed in T22.
