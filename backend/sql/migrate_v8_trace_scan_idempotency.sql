-- =====================================================
-- Migration V8: trace scan idempotency table
-- Date: 2026-05-05
-- Purpose:
--   Persist (trace_code + action_type + idempotency_key) so repeated scan
--   submissions do not create duplicate lifecycle logs.
-- Safety:
--   CREATE TABLE IF NOT EXISTS is re-runnable and does not rewrite existing
--   trace lifecycle data.
-- =====================================================

CREATE TABLE IF NOT EXISTS trace_scan_idempotency (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Idempotency record id',
  trace_code VARCHAR(64) NOT NULL COMMENT 'Trace code',
  action_type VARCHAR(32) NOT NULL COMMENT 'Scan action type',
  idempotency_key VARCHAR(64) NOT NULL COMMENT 'Client idempotency key',
  lifecycle_log_id BIGINT NULL COMMENT 'Lifecycle log id produced by first successful request',
  status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/SUCCEEDED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_scan_idempotency (trace_code, action_type, idempotency_key),
  INDEX idx_trace_scan_idempotency_log_id (lifecycle_log_id),

  CONSTRAINT fk_trace_scan_idempotency_log
    FOREIGN KEY (lifecycle_log_id) REFERENCES trace_lifecycle_log(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Trace scan idempotency records';

SELECT 'Trace scan idempotency migration v8 completed' AS result;
