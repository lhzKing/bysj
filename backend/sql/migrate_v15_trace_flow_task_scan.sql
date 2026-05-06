-- B19 task-internal continuous scan support.
-- Stores one successful scan detail per task + trace code + action so repeated
-- scanner reads can return "already scanned" feedback without re-counting.

CREATE TABLE IF NOT EXISTS trace_flow_task_scan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'flow task scan detail id',
  task_id BIGINT NOT NULL COMMENT 'flow task id',
  trace_code VARCHAR(64) NOT NULL COMMENT 'scanned single-item trace code',
  action_type VARCHAR(32) NOT NULL COMMENT 'OUTBOUND/INBOUND/TRANSFER action recorded for this task scan',
  counted TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 if this scan counts toward task actual_quantity',
  operator_user_id BIGINT NULL COMMENT 'scan operator user id',
  operator_username VARCHAR(64) NULL COMMENT 'scan operator username',
  idempotency_key VARCHAR(64) NULL COMMENT 'stable task scan idempotency key',
  scan_time DATETIME NOT NULL COMMENT 'task scan time',
  duplicate_count INT NOT NULL DEFAULT 0 COMMENT 'repeat scans after first success',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_flow_task_scan_code_action (task_id, trace_code, action_type),
  INDEX idx_trace_flow_task_scan_task_time (task_id, scan_time),
  INDEX idx_trace_flow_task_scan_trace_code (trace_code),
  INDEX idx_trace_flow_task_scan_operator (operator_user_id),

  CONSTRAINT fk_trace_flow_task_scan_task FOREIGN KEY (task_id) REFERENCES trace_flow_task(id) ON DELETE CASCADE,
  CONSTRAINT fk_trace_flow_task_scan_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT ck_trace_flow_task_scan_action CHECK (action_type IN ('OUTBOUND','INBOUND','TRANSFER')),
  CONSTRAINT ck_trace_flow_task_scan_counted CHECK (counted IN (0, 1)),
  CONSTRAINT ck_trace_flow_task_scan_duplicate_count CHECK (duplicate_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='flow task scanned code detail for continuous scan';
