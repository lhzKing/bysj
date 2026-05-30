# Phase 1: Scope Audit

- [x] Audit `regions.js` current mixed responsibilities.
- [x] Confirm current consumers are frozen frontend components.
- [x] Lock T22 to shared-data refactor only.

## Notes

- `InboundForm.vue`, `OutboundForm.vue`, and `TransferForm.vue` still import `@/shared/data/regions` directly.
- To stay inside the freeze boundary, T22 does not change component imports or form logic.
