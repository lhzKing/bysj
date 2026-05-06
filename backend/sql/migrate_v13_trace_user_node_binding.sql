-- B15 user-to-trace-node binding model.
-- Users can be bound to one or more enabled trace_node records. Normal scan
-- movement can then be authorized by the current user's operable node.

CREATE TABLE IF NOT EXISTS trace_user_node_binding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'user-node binding id',
  user_id BIGINT NOT NULL COMMENT 'bound user id',
  node_id BIGINT NOT NULL COMMENT 'bound trace node id',
  org_id BIGINT NULL COMMENT 'organization id copied from trace_node.org_id',
  default_node TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 default operation node for the user',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_user_node_binding (user_id, node_id),
  INDEX idx_trace_user_node_user_enabled (user_id, enabled),
  INDEX idx_trace_user_node_node_id (node_id),
  INDEX idx_trace_user_node_org_id (org_id),

  CONSTRAINT fk_trace_user_node_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_trace_user_node_node FOREIGN KEY (node_id) REFERENCES trace_node(id) ON DELETE CASCADE,
  CONSTRAINT ck_trace_user_node_default CHECK (default_node IN (0, 1)),
  CONSTRAINT ck_trace_user_node_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user operable trace node binding';
