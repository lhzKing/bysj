# T04 TraceSnapshotMapper Cleanup

- Task ID: T04
- Created At: 2026-04-14 11:20
- Status: DONE
- Source: `项目整改执行任务表.md`

## Task Definition

Clean up the residual `TraceSnapshotMapper` interface/XML method that was declared for a `FOR UPDATE`
query but no longer fits the current optimistic-lock based trace update flow.

## Goals

1. Confirm whether `selectByTraceCodeForUpdate(...)` should be deleted or truly adopted.
2. Align mapper surface with the actual trace snapshot read/update strategy.
3. Remove dead mapper/XML residue without expanding scope.
4. Preserve current trace assignment / scan / detail behavior.

## Scope

### In Scope

- `backend/src/main/java/com/example/trace/mapper/TraceSnapshotMapper.java`
- `backend/src/main/resources/mapper/TraceMapper.xml`
- Trace snapshot call sites that prove whether the method is active
- Minimal verification for trace scan/detail paths

### Out of Scope

- Frontend changes
- Trace business redesign
- Database schema changes
- Non-mapper cleanup unrelated to `TraceSnapshotMapper`

## Initial Finding

`TraceSnapshotMapper` exposes `selectByTraceCodeForUpdate(traceCode)`, backed by `TraceMapper.xml`, but the
current scan/detail/update flow uses `selectById(...)` plus `@Version` optimistic locking and retry logic.
This strongly suggests the custom `FOR UPDATE` method is historical residue.
