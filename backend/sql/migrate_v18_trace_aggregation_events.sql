-- V18: document packaging/pallet lifecycle event action types for existing deployments.
-- The current schema stores trace_lifecycle_log.action_type as VARCHAR(32) without a CHECK constraint,
-- so no data rewrite is required. This migration aligns the column comment with B26 ActionType values.

ALTER TABLE trace_lifecycle_log
  MODIFY COLUMN action_type VARCHAR(32) NOT NULL
  COMMENT 'INIT/PRINT_CODE/REPRINT_CODE/ACTIVATE_CODE/VOID_CODE/PACK/UNPACK/PALLETIZE/UNPALLETIZE/INBOUND/OUTBOUND/TRANSFER/EXCEPTION/CORRECTION';
