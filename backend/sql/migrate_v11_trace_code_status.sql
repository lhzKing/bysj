-- =====================================================
-- Migration V11: single-item trace-code status model
-- Date: 2026-05-05
-- Scope:
--   1. Create trace_code as the code/label status table for one-item-one-code.
--   2. Backfill existing snapshot-only trace codes as ACTIVATED-compatible rows.
--   3. Keep batch_id nullable so legacy produceAssign rows and B08-only deployments remain valid.
-- =====================================================

CREATE TABLE IF NOT EXISTS trace_code (
  trace_code VARCHAR(64) PRIMARY KEY COMMENT '单品溯源码',
  batch_id BIGINT NULL COMMENT '所属赋码批次ID；兼容历史单码赋码允许为空',
  spu_id BIGINT NOT NULL COMMENT '关联SPU',
  serial_no INT NULL COMMENT '批次内序号',
  qr_payload VARCHAR(512) NOT NULL COMMENT '二维码载荷',
  code_status VARCHAR(32) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED/PRINTED/ACTIVATED/IN_STOCK/IN_TRANSIT/EXCEPTION/VOIDED/SCRAPPED',
  print_count INT NOT NULL DEFAULT 0 COMMENT '打印/重打次数',
  activated_time DATETIME NULL COMMENT '激活时间',
  activated_by BIGINT NULL COMMENT '激活操作人ID',
  activated_by_username VARCHAR(64) NULL COMMENT '激活操作人用户名',
  current_snapshot_id VARCHAR(64) NULL COMMENT '当前快照指针；当前等于 trace_snapshot.trace_code',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单品码状态表';

INSERT INTO trace_code (
  trace_code,
  batch_id,
  spu_id,
  serial_no,
  qr_payload,
  code_status,
  print_count,
  activated_time,
  current_snapshot_id,
  create_time,
  update_time
)
SELECT
  s.trace_code,
  NULL,
  s.spu_id,
  NULL,
  s.trace_code,
  CASE
    WHEN s.current_status = 'IN_STOCK' THEN 'IN_STOCK'
    WHEN s.current_status IN ('IN_TRANSIT', 'TRANSFERRED') THEN 'IN_TRANSIT'
    WHEN s.current_status = 'EXCEPTION' THEN 'EXCEPTION'
    ELSE 'ACTIVATED'
  END,
  0,
  s.last_event_time,
  s.trace_code,
  COALESCE(s.last_event_time, NOW()),
  NOW()
FROM trace_snapshot s
LEFT JOIN trace_code c ON c.trace_code = s.trace_code
WHERE c.trace_code IS NULL;

SELECT 'Trace code status table migration completed' AS result;
