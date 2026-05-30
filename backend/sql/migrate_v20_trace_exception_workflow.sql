-- B30 exception and correction workflow businessization.
-- Adds restore fields on trace_snapshot so EXCEPTION_OPEN can freeze the item
-- and EXCEPTION_CLOSE can restore the pre-freeze status/node/owner.

SET NAMES utf8mb4;

ALTER TABLE trace_snapshot
  ADD COLUMN exception_restore_status VARCHAR(32) NULL
    COMMENT 'pre-exception lifecycle status restored by EXCEPTION_CLOSE' AFTER current_owner,
  ADD COLUMN exception_restore_node VARCHAR(64) NULL
    COMMENT 'pre-exception current node restored by EXCEPTION_CLOSE' AFTER exception_restore_status,
  ADD COLUMN exception_restore_owner VARCHAR(64) NULL
    COMMENT 'pre-exception current owner restored by EXCEPTION_CLOSE' AFTER exception_restore_node;

ALTER TABLE trace_lifecycle_log
  MODIFY COLUMN action_type VARCHAR(32) NOT NULL
    COMMENT 'INIT/PRINT_CODE/REPRINT_CODE/ACTIVATE_CODE/VOID_CODE/PACK/UNPACK/PALLETIZE/UNPALLETIZE/INBOUND/OUTBOUND/TRANSFER/DELIVER/EXCEPTION/EXCEPTION_OPEN/EXCEPTION_CLOSE/CORRECTION';
