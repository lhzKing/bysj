-- B16 trace flow task model.
-- A flow task acts like a waybill/shipping task: it fixes source node, target
-- node, expected quantity, task type, and lifecycle status before task-driven
-- scanning is implemented in B17-B20.

CREATE TABLE IF NOT EXISTS trace_flow_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'flow task id',
  task_no VARCHAR(64) NOT NULL COMMENT 'flow task number',
  task_type VARCHAR(32) NOT NULL COMMENT 'OUTBOUND/TRANSFER/INBOUND/RECEIVE',
  source_node_id BIGINT NOT NULL COMMENT 'source trace node id',
  target_node_id BIGINT NOT NULL COMMENT 'target trace node id',
  expected_quantity INT NOT NULL COMMENT 'expected scan/flow quantity',
  actual_quantity INT NOT NULL DEFAULT 0 COMMENT 'actual scan/flow quantity',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PROCESSING/COMPLETED/CANCELLED/EXCEPTION',
  create_by BIGINT NULL COMMENT 'creator user id',
  create_by_username VARCHAR(64) NULL COMMENT 'creator username',
  complete_time DATETIME NULL COMMENT 'completion time',
  cancel_time DATETIME NULL COMMENT 'cancellation time',
  remark VARCHAR(255) NULL COMMENT 'business remark',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_flow_task_no (task_no),
  INDEX idx_trace_flow_task_type_status (task_type, status),
  INDEX idx_trace_flow_task_source_node (source_node_id),
  INDEX idx_trace_flow_task_target_node (target_node_id),
  INDEX idx_trace_flow_task_create_by (create_by),
  INDEX idx_trace_flow_task_create_time (create_time),

  CONSTRAINT fk_trace_flow_task_source_node FOREIGN KEY (source_node_id) REFERENCES trace_node(id) ON DELETE RESTRICT,
  CONSTRAINT fk_trace_flow_task_target_node FOREIGN KEY (target_node_id) REFERENCES trace_node(id) ON DELETE RESTRICT,
  CONSTRAINT fk_trace_flow_task_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT ck_trace_flow_task_type CHECK (task_type IN ('OUTBOUND','TRANSFER','INBOUND','RECEIVE')),
  CONSTRAINT ck_trace_flow_task_status CHECK (status IN ('CREATED','PROCESSING','COMPLETED','CANCELLED','EXCEPTION')),
  CONSTRAINT ck_trace_flow_task_expected_quantity CHECK (expected_quantity > 0),
  CONSTRAINT ck_trace_flow_task_actual_quantity CHECK (actual_quantity >= 0),
  CONSTRAINT ck_trace_flow_task_distinct_nodes CHECK (source_node_id <> target_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trace flow task / shipping waybill';
