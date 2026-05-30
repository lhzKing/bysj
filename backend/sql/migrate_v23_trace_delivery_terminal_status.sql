-- =====================================================
-- Migration V23: terminal delivery lifecycle semantics
-- Date: 2026-05-31
-- Scope:
--   1) Existing deployments created by V11 mirrored snapshot TRANSFERRED
--      rows as trace_code.IN_TRANSIT. That kept delivered physical items
--      eligible for later inbound scans. Align code_status with the terminal
--      lifecycle state.
--   2) Refresh column comments so operational schema documents DELIVER and
--      trace_code.TRANSFERRED.
-- =====================================================

ALTER TABLE trace_code
  MODIFY COLUMN code_status VARCHAR(32) NOT NULL DEFAULT 'GENERATED'
  COMMENT 'GENERATED/PRINTED/ACTIVATED/IN_STOCK/IN_TRANSIT/TRANSFERRED/EXCEPTION/VOIDED/SCRAPPED';

ALTER TABLE trace_lifecycle_log
  MODIFY COLUMN action_type VARCHAR(32) NOT NULL
  COMMENT 'INIT/PRINT_CODE/REPRINT_CODE/ACTIVATE_CODE/VOID_CODE/PACK/UNPACK/PALLETIZE/UNPALLETIZE/INBOUND/OUTBOUND/TRANSFER/DELIVER/EXCEPTION/EXCEPTION_OPEN/EXCEPTION_CLOSE/CORRECTION';

UPDATE trace_code c
JOIN trace_snapshot s ON s.trace_code = c.trace_code
SET c.code_status = 'TRANSFERRED'
WHERE s.current_status = 'TRANSFERRED'
  AND c.code_status = 'IN_TRANSIT';

SELECT 'Migration v23 (terminal delivery lifecycle semantics) completed' AS result;
