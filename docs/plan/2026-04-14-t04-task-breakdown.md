# T04 Task Breakdown

## Goal

Remove the stale `TraceSnapshotMapper` custom `FOR UPDATE` method and align mapper artifacts with the actual optimistic-lock flow.

## Phase 1: Audit & Boundary Lock

- [x] T04-1.1 Inspect mapper/interface/xml residue and active callers.
- [x] T04-1.2 Confirm cleanup strategy: delete stale method instead of reviving pessimistic locking.

## Phase 2: Mapper Cleanup

- [x] T04-2.1 Remove `selectByTraceCodeForUpdate(...)` from `TraceSnapshotMapper`.
- [x] T04-2.2 Remove the orphan XML mapping if no custom snapshot SQL remains.

## Phase 3: Verification

- [x] T04-3.1 Run focused tests for trace scan/detail/demo-data flows.
- [x] T04-3.2 Confirm no code references the deleted method.

## Phase 4: Documentation Closeout

- [x] T04-4.1 Update MASTER and phase progress files.
- [x] T04-4.2 Update the root task table and append an update record.
