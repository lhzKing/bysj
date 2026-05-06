-- B14: Structured trace business nodes.
-- Existing deployments get the trace_node master-data table. The optional
-- trace_assign_batch.manufacturer_node_id relationship is kept nullable and
-- will be validated by later node/task-flow work after existing data is clean.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS trace_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'trace node id',
  node_code VARCHAR(64) NOT NULL COMMENT 'node code',
  node_name VARCHAR(64) NOT NULL COMMENT 'node name',
  node_type VARCHAR(32) NOT NULL COMMENT 'FACTORY/WAREHOUSE/LOGISTICS/CUSTOMER/SERVICE',
  org_id BIGINT NULL COMMENT 'organization id reserved for B15',
  province VARCHAR(32) NOT NULL COMMENT 'province',
  city VARCHAR(32) NOT NULL COMMENT 'city',
  address VARCHAR(255) NULL COMMENT 'address',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_node_code (node_code),
  INDEX idx_trace_node_type (node_type),
  INDEX idx_trace_node_enabled (enabled),
  INDEX idx_trace_node_org_id (org_id),
  INDEX idx_trace_node_region (province, city),

  CONSTRAINT ck_trace_node_enabled CHECK (enabled IN (0, 1)),
  CONSTRAINT ck_trace_node_type CHECK (node_type IN ('FACTORY','WAREHOUSE','LOGISTICS','CUSTOMER','SERVICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='structured trace business node';
