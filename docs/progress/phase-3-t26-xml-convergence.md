# Phase 3: XML Mapper Convergence

- [x] Extract reusable lifecycle-log column fragments in XML.

## Notes

- Added `TraceLifecycleLogColumns` and `TraceLifecycleLogColumnsWithAlias` SQL fragments.
- Replaced repeated column projections in `selectLatestByTraceCode`, `selectEffectiveHistory`, and `selectFullChain`.
