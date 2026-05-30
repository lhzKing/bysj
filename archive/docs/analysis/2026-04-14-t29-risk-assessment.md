# T29 Risk Assessment

## Primary Risks

1. **Legacy external clients**
   - Removing old trace endpoints could affect external callers outside the repository.
   - Mitigation: scope based on repository usage audit; keep current RESTful endpoints unchanged; document the inference explicitly.

2. **Response payload drift**
   - Removing the old `LoginResponse` constructor must not alter current payload shape.
   - Mitigation: migrate call sites to the 4-arg constructor with explicit empty permissions.

3. **Over-cleaning useful utilities**
   - Some apparently unused methods may still be useful for future work.
   - Mitigation: remove only clear historical compatibility stubs and zero-reference residue.

## Decision Boundary

- Clean items only when repository search shows no active call sites or when callers can be migrated directly in the same change set.
- Do not touch blocked frontend component work (T23) or broader frontend UI files.
