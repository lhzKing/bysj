-- B25 carton/pallet aggregation model.
-- Aggregation codes are operation parents only: every physical item keeps its
-- own trace_code, while carton/pallet parent codes allow later B27 batch scans.

CREATE TABLE IF NOT EXISTS trace_aggregation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'aggregation relation id',
  parent_code VARCHAR(64) NOT NULL COMMENT 'carton/pallet parent aggregation code',
  child_code VARCHAR(64) NOT NULL COMMENT 'child aggregation code or single-item trace code',
  relation_type VARCHAR(32) NOT NULL COMMENT 'CARTON/PALLET/BATCH',
  active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 active, 0 released',
  active_marker TINYINT GENERATED ALWAYS AS (CASE WHEN active = 1 THEN 1 ELSE NULL END) STORED,
  create_by BIGINT NULL COMMENT 'binding operator user id',
  create_by_username VARCHAR(64) NULL COMMENT 'binding operator username',
  bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'relation bind time',
  release_time DATETIME NULL COMMENT 'relation release time',
  remark VARCHAR(255) NULL COMMENT 'business remark',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_aggregation_active_pair (parent_code, child_code, active_marker),
  INDEX idx_trace_aggregation_parent_active (parent_code, active),
  INDEX idx_trace_aggregation_child_active (child_code, active),
  INDEX idx_trace_aggregation_type_active (relation_type, active),
  INDEX idx_trace_aggregation_create_by (create_by),

  CONSTRAINT fk_trace_aggregation_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT ck_trace_aggregation_relation_type CHECK (relation_type IN ('CARTON','PALLET','BATCH')),
  CONSTRAINT ck_trace_aggregation_active CHECK (active IN (0, 1)),
  CONSTRAINT ck_trace_aggregation_distinct_codes CHECK (parent_code <> child_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='carton/pallet aggregation relation';
