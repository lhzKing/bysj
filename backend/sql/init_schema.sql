-- Fresh schema for trace supply-chain system

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS trace_flow_task_scan;
DROP TABLE IF EXISTS trace_aggregation;
DROP TABLE IF EXISTS trace_scan_idempotency;
DROP TABLE IF EXISTS trace_lifecycle_log;
DROP TABLE IF EXISTS trace_snapshot;
DROP TABLE IF EXISTS trace_code;
DROP TABLE IF EXISTS trace_assign_batch;
DROP TABLE IF EXISTS trace_flow_task;
DROP TABLE IF EXISTS trace_user_node_binding;
DROP TABLE IF EXISTS trace_node;
DROP TABLE IF EXISTS base_part_spec;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

-- ==================== Identity and permission ====================

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(32) NOT NULL UNIQUE COMMENT 'role code',
  role_name VARCHAR(64) NOT NULL COMMENT 'role name',
  remark VARCHAR(255) COMMENT 'remark',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role';

CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  perm_code VARCHAR(64) NOT NULL UNIQUE COMMENT 'permission code',
  perm_name VARCHAR(64) NOT NULL COMMENT 'permission name',
  api_method VARCHAR(10) COMMENT 'HTTP method: GET/POST/PUT/DELETE/*',
  api_pattern VARCHAR(128) COMMENT 'API path pattern, supports * wildcard',
  remark VARCHAR(255) COMMENT 'remark',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='permission';

CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role permission relation';

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  role_id BIGINT NOT NULL COMMENT 'role id',
  token_version INT NOT NULL DEFAULT 0 COMMENT 'token invalidation version',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user';

-- ==================== SPU master data ====================

CREATE TABLE base_part_spec (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'SPU id',
  part_code VARCHAR(64) NOT NULL UNIQUE COMMENT 'part code',
  part_name VARCHAR(128) NOT NULL COMMENT 'part name',
  part_type VARCHAR(64) NOT NULL COMMENT 'part type',
  model VARCHAR(64) COMMENT 'model',
  manufacturer VARCHAR(128) COMMENT 'manufacturer',
  unit VARCHAR(32) COMMENT 'unit',
  remark VARCHAR(255) COMMENT 'remark',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='part specification SPU';

-- ==================== Structured business nodes ====================

CREATE TABLE trace_node (
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

CREATE TABLE trace_user_node_binding (
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

CREATE TABLE trace_flow_task (
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
  discrepancy_type VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/SHORTAGE/OVERAGE completion discrepancy type',
  discrepancy_quantity INT NOT NULL DEFAULT 0 COMMENT 'absolute difference between expected and actual quantity',
  discrepancy_reason VARCHAR(255) NULL COMMENT 'required reason when expected and actual quantity differ',
  discrepancy_time DATETIME NULL COMMENT 'time when discrepancy was recorded',
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
  CONSTRAINT ck_trace_flow_task_discrepancy_type CHECK (discrepancy_type IN ('NONE','SHORTAGE','OVERAGE')),
  CONSTRAINT ck_trace_flow_task_expected_quantity CHECK (expected_quantity > 0),
  CONSTRAINT ck_trace_flow_task_actual_quantity CHECK (actual_quantity >= 0),
  CONSTRAINT ck_trace_flow_task_discrepancy_quantity CHECK (discrepancy_quantity >= 0),
  CONSTRAINT ck_trace_flow_task_distinct_nodes CHECK (source_node_id <> target_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trace flow task / shipping waybill';

CREATE TABLE trace_flow_task_scan (
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

-- ==================== Aggregation code hierarchy ====================

CREATE TABLE trace_aggregation (
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

-- ==================== Trace assignment and code status ====================

CREATE TABLE trace_assign_batch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'assign batch id',
  batch_no VARCHAR(64) NOT NULL COMMENT 'assign batch number',
  production_order_no VARCHAR(64) NULL COMMENT 'production order number',
  spu_id BIGINT NOT NULL COMMENT 'related SPU',
  quantity_requested INT NOT NULL COMMENT 'requested quantity',
  quantity_generated INT NOT NULL DEFAULT 0 COMMENT 'generated quantity',
  quantity_printed INT NOT NULL DEFAULT 0 COMMENT 'printed quantity',
  quantity_activated INT NOT NULL DEFAULT 0 COMMENT 'activated quantity',
  manufacturer_node_id BIGINT NULL COMMENT 'manufacturer node id reserved for B14 trace_node',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/GENERATING/GENERATED/PARTIAL_FAILED/FAILED/CANCELLED',
  operator_id BIGINT NULL COMMENT 'operator id',
  operator_username VARCHAR(64) NULL COMMENT 'operator username',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_assign_batch_no (batch_no),
  INDEX idx_trace_assign_batch_spu_id (spu_id),
  INDEX idx_trace_assign_batch_order_no (production_order_no),
  INDEX idx_trace_assign_batch_status (status),
  INDEX idx_trace_assign_batch_operator_id (operator_id),
  INDEX idx_trace_assign_batch_manufacturer_node_id (manufacturer_node_id),

  CONSTRAINT fk_trace_assign_batch_spu FOREIGN KEY (spu_id) REFERENCES base_part_spec(id) ON DELETE RESTRICT,
  CONSTRAINT fk_trace_assign_batch_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT fk_trace_assign_batch_manufacturer_node FOREIGN KEY (manufacturer_node_id) REFERENCES trace_node(id) ON DELETE SET NULL,
  CONSTRAINT ck_trace_assign_batch_quantity_requested CHECK (quantity_requested > 0),
  CONSTRAINT ck_trace_assign_batch_quantity_generated CHECK (quantity_generated >= 0 AND quantity_generated <= quantity_requested),
  CONSTRAINT ck_trace_assign_batch_quantity_printed CHECK (quantity_printed >= 0 AND quantity_printed <= quantity_requested),
  CONSTRAINT ck_trace_assign_batch_quantity_activated CHECK (quantity_activated >= 0 AND quantity_activated <= quantity_requested)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trace assign batch';

CREATE TABLE trace_code (
  trace_code VARCHAR(64) PRIMARY KEY COMMENT 'single-item trace code',
  batch_id BIGINT NULL COMMENT 'assign batch id; nullable for legacy single-code assignment',
  spu_id BIGINT NOT NULL COMMENT 'related SPU',
  serial_no INT NULL COMMENT 'serial number within batch',
  qr_payload VARCHAR(512) NOT NULL COMMENT 'QR payload',
  code_status VARCHAR(32) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED/PRINTED/ACTIVATED/IN_STOCK/IN_TRANSIT/EXCEPTION/VOIDED/SCRAPPED',
  print_count INT NOT NULL DEFAULT 0 COMMENT 'print/reprint count',
  activated_time DATETIME NULL COMMENT 'activation time',
  activated_by BIGINT NULL COMMENT 'activation operator id',
  activated_by_username VARCHAR(64) NULL COMMENT 'activation operator username',
  current_snapshot_id VARCHAR(64) NULL COMMENT 'current snapshot pointer; currently equals trace_snapshot.trace_code',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_trace_code_batch_id (batch_id),
  INDEX idx_trace_code_spu_id (spu_id),
  INDEX idx_trace_code_status (code_status),
  INDEX idx_trace_code_activated_by (activated_by),
  UNIQUE KEY uk_trace_code_batch_serial (batch_id, serial_no),

  CONSTRAINT fk_trace_code_batch FOREIGN KEY (batch_id) REFERENCES trace_assign_batch(id) ON DELETE SET NULL,
  CONSTRAINT fk_trace_code_spu FOREIGN KEY (spu_id) REFERENCES base_part_spec(id) ON DELETE RESTRICT,
  CONSTRAINT fk_trace_code_activated_by FOREIGN KEY (activated_by) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT ck_trace_code_serial_no CHECK (serial_no IS NULL OR serial_no > 0),
  CONSTRAINT ck_trace_code_print_count CHECK (print_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trace code status';

-- ==================== Trace lifecycle ====================

CREATE TABLE trace_snapshot (
  trace_code VARCHAR(64) PRIMARY KEY COMMENT 'trace code',
  spu_id BIGINT NOT NULL COMMENT 'related SPU',

  current_status VARCHAR(32) NOT NULL COMMENT 'current lifecycle status',
  current_node VARCHAR(64) COMMENT 'current node',
  current_owner VARCHAR(64) COMMENT 'current owner',

  province VARCHAR(32) COMMENT 'province',
  city VARCHAR(32) COMMENT 'city',

  last_event_time DATETIME NOT NULL COMMENT 'last event time',
  last_log_id BIGINT NOT NULL COMMENT 'last lifecycle log id',
  last_hash CHAR(64) NOT NULL COMMENT 'latest chain hash',

  version INT NOT NULL DEFAULT 0 COMMENT 'optimistic lock version',

  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_trace_snapshot_spu_id (spu_id),
  -- T-P1-03: supports Dashboard selectKpi `today_new` count after rewriting
  -- DATE(last_event_time)=CURDATE() to a sargable range filter.
  INDEX idx_trace_snapshot_last_event_time (last_event_time),
  CONSTRAINT fk_trace_snapshot_spu FOREIGN KEY (spu_id) REFERENCES base_part_spec(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trace snapshot';

CREATE TABLE trace_lifecycle_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'lifecycle log id',
  trace_code VARCHAR(64) NOT NULL COMMENT 'trace code',
  spu_id BIGINT NOT NULL COMMENT 'related SPU',

  action_type VARCHAR(32) NOT NULL COMMENT 'INIT/PRINT_CODE/REPRINT_CODE/ACTIVATE_CODE/VOID_CODE/PACK/UNPACK/PALLETIZE/UNPALLETIZE/INBOUND/OUTBOUND/TRANSFER/EXCEPTION/CORRECTION',

  from_node VARCHAR(64) COMMENT 'source node',
  to_node VARCHAR(64) COMMENT 'target node',

  province VARCHAR(32) COMMENT 'province',
  city VARCHAR(32) COMMENT 'city',
  remark VARCHAR(255) COMMENT 'remark',

  event_time DATETIME NOT NULL COMMENT 'event time',
  ingest_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ingest time',

  prev_hash CHAR(64) NOT NULL COMMENT 'previous hash',
  current_hash CHAR(64) NOT NULL COMMENT 'current hash',

  correction_of BIGINT NULL COMMENT 'corrected lifecycle log id',

  operator VARCHAR(64) NOT NULL COMMENT 'operator protected by B06 hash/signature',

  signature VARCHAR(512) COMMENT 'RSA signature in Base64',
  signature_key_id VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'signature key id',
  signature_key_version INT NOT NULL DEFAULT 1 COMMENT 'signature key version',

  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  INDEX idx_trace_code (trace_code),
  INDEX idx_event_time (event_time),
  INDEX idx_ingest_time (ingest_time),
  INDEX idx_province (province),
  INDEX idx_from_to (from_node, to_node),
  INDEX idx_correction_of (correction_of),
  INDEX idx_trace_lifecycle_log_spu_id (spu_id),
  INDEX idx_signature_key (signature_key_id, signature_key_version),
  -- T-P1-03: supports Dashboard selectKpi `exception_count` SUM and any future
  -- action-type-filtered analytics. Single-column action_type has low cardinality
  -- (lifecycle action values) but is still selective for EXCEPTION/CORRECTION rows that
  -- typically make up <5% of total log volume.
  INDEX idx_action_type (action_type),

  CONSTRAINT fk_trace_log_spu FOREIGN KEY (spu_id) REFERENCES base_part_spec(id) ON DELETE RESTRICT,
  CONSTRAINT fk_correction_of FOREIGN KEY (correction_of) REFERENCES trace_lifecycle_log(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trace lifecycle log';

CREATE TABLE trace_scan_idempotency (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'idempotency record id',
  trace_code VARCHAR(64) NOT NULL COMMENT 'trace code',
  action_type VARCHAR(32) NOT NULL COMMENT 'action type',
  idempotency_key VARCHAR(64) NOT NULL COMMENT 'idempotency key',
  lifecycle_log_id BIGINT NULL COMMENT 'lifecycle log id after success',
  status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/SUCCEEDED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_scan_idempotency (trace_code, action_type, idempotency_key),
  INDEX idx_trace_scan_idempotency_log_id (lifecycle_log_id),

  CONSTRAINT fk_trace_scan_idempotency_log
    FOREIGN KEY (lifecycle_log_id) REFERENCES trace_lifecycle_log(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='trace scan idempotency';

-- ==================== Seed data ====================

INSERT INTO sys_role (id, role_code, role_name, remark) VALUES
(1, 'SUPER_ADMIN', 'Super Admin', 'System built-in super administrator'),
(2, 'ADMIN', 'Admin', 'System administrator'),
(3, 'PRODUCER', 'Producer', 'Production trace-code assignment operator'),
(4, 'WAREHOUSE', 'Warehouse', 'Warehouse inbound/outbound operator'),
(5, 'LOGISTICS', 'Logistics', 'Logistics transfer operator'),
(6, 'USER', 'User', 'Read-only trace user');

INSERT INTO sys_permission (id, perm_code, perm_name, api_method, api_pattern, remark) VALUES
-- Trace
(1, 'trace:create', 'Create trace code', 'POST', '/api/traces', 'Create trace instances'),
(2, 'trace:scan', 'Scan trace code', 'POST', '/api/traces/*/events', 'Super scan permission for all scan actions'),
(3, 'trace:view', 'View trace detail', 'GET', '/api/traces/*', 'View trace detail'),
(13, 'trace:inbound', 'Inbound scan', 'POST', '/api/traces/*/events', 'Allow inbound scan action'),
(14, 'trace:outbound', 'Outbound scan', 'POST', '/api/traces/*/events', 'Allow outbound scan action'),
(15, 'trace:transfer', 'Transfer scan', 'POST', '/api/traces/*/events', 'Allow transfer scan action'),
(16, 'trace:audit:view', 'Trace audit view', NULL, NULL, 'View full trace audit history; checked by view=audit'),
(17, 'trace:batch:create', 'Create trace assignment batch', 'POST', '/api/traces', 'Create assignment batches and generated single-item trace codes'),
(18, 'trace:code:print', 'Print trace code label', NULL, NULL, 'Print, reprint, or void trace-code labels; checked by controller annotations'),
(19, 'trace:code:activate', 'Activate trace code', 'POST', '/api/trace-codes/*/activate', 'Activate or review a single trace code after label attachment'),
(20, 'trace:task:create', 'Create trace flow task', 'POST', '/api/trace-flow-tasks', 'Create warehouse/logistics flow tasks'),
(21, 'trace:task:scan', 'Scan trace flow task', 'POST', '/api/trace-flow-tasks/*/scan', 'Scan single, carton, or pallet codes inside flow tasks'),
(22, 'trace:task:complete', 'Complete trace flow task', 'POST', '/api/trace-flow-tasks/*/complete', 'Complete flow tasks and record discrepancy handling'),
(23, 'trace:exception:handle', 'Handle trace exception', NULL, NULL, 'Report or handle trace lifecycle exceptions; checked by action policy'),
-- Dashboard
(4, 'dashboard:view', 'View dashboard', 'GET', '/api/dashboard/*', 'View dashboard statistics'),
-- User management
(5, 'user:view', 'View users', 'GET', '/api/users/*', 'View users'),
(6, 'user:manage', 'Manage users', '*', '/api/users/*', 'Create update and delete users'),
-- Role management
(7, 'role:view', 'View roles', 'GET', '/api/roles/*', 'View roles and permissions'),
(8, 'role:manage', 'Manage roles', '*', '/api/roles/*', 'Create update delete roles and assign permissions'),
-- Part management
(9, 'part:view', 'View parts', 'GET', '/api/parts/*', 'View parts'),
(10, 'part:manage', 'Manage parts', '*', '/api/parts/*', 'Create update and delete parts'),
-- Demo data management
(11, 'trace:data:generate', 'Generate demo data', 'POST', '/api/admin/generate-sample-data', 'Generate demo trace data'),
(12, 'trace:data:clear', 'Clear trace data', 'DELETE', '/api/admin/clear-trace-data', 'Dangerous operation to clear trace logs and snapshots');

-- SUPER_ADMIN has all permissions
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- ADMIN has all permissions
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission;

-- PRODUCER: create trace + view trace + dashboard + part view
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 1), (3, 3), (3, 4), (3, 9), (3, 17), (3, 18), (3, 19), (3, 23);

-- WAREHOUSE: inbound/outbound + trace view + dashboard + part view
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(4, 13), (4, 14), (4, 3), (4, 4), (4, 9), (4, 20), (4, 21), (4, 22), (4, 23);

-- LOGISTICS: transfer + trace view + dashboard
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(5, 15), (5, 3), (5, 4), (5, 20), (5, 21), (5, 22), (5, 23);

-- USER: trace view + dashboard
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(6, 3), (6, 4);

INSERT INTO sys_user (username, password, role_id) VALUES
('superadmin', '$2a$10$3Z07pHScGSUSEWVGzSVlTeKc2GgI.GXw.Ui1moglCS..BWH.NN8e6', 1),
('admin', '$2a$10$Hb9w2vKs8n87UofMAtQ39.VM7eORPX2cc4OQ9ARkyGenz//WJUPMa', 2),
('producer', '$2a$10$w/IJTFiAeHeMkwCICp7mau61qFoQbTImLaNmlXgCtHL7GmFxnoHLe', 3),
('warehouse', '$2a$10$o5ktsd5zQuKsvZUwjOlMZeyMx6t.yFDfG8etQOu5qgO9lHGqgibmy', 4),
('logistics', '$2a$10$r2lXyB75LpI9SfLYv5vRaeSQG7GMuFYapGoAu1elSWd7cp5nnvEb2', 5),
('user', '$2a$10$vm25NbGJuiFPX0kRzxkCvOgvOaeWxPeCev9VXUpSpO5F33x8t7FE6', 6);

INSERT INTO base_part_spec (part_code, part_name, part_type, model, manufacturer, unit, remark)
VALUES ('SPU-VALVE-001', 'Valve Assembly', 'Valve', 'V-2024001', 'Sample Manufacturer', 'piece', 'Seed SPU for demo and development');

SET FOREIGN_KEY_CHECKS = 1;
