# T22 Risk Assessment

## Main Risks

1. **Import path breakage**
   - Risk: frozen components still import `@/shared/data/regions`
   - Mitigation: keep `regions.js` as a stable compatibility barrel

2. **Lookup behavior drift**
   - Risk: auto-fill behavior would change if `getRegionByNode(...)` changes
   - Mitigation: preserve exact-match-first and `key.substring(0, 2)` fuzzy-match fallback behavior; lock it with tests

3. **Scope expansion into UI files**
   - Risk: shared-data cleanup could spill into component refactors
   - Mitigation: keep T22 limited to shared data modules, tests, docs, and skill files
