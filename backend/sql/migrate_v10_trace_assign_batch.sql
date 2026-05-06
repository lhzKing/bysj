-- =====================================================
-- 迁移脚本 V10: 赋码批次基础表
-- 日期: 2026-05-05
-- 说明:
--   1. 新增 trace_assign_batch，承载生产计划/工单维度的赋码批次
--   2. 先落地批次容器与数量字段；B10-B13 再接入真实赋码、打印、激活和对账流程
--   3. manufacturer_node_id 预留给 B14 trace_node，当前不加外键
-- =====================================================

CREATE TABLE IF NOT EXISTS trace_assign_batch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '赋码批次ID',
  batch_no VARCHAR(64) NOT NULL COMMENT '赋码批次号',
  production_order_no VARCHAR(64) NULL COMMENT '生产计划/工单号',
  spu_id BIGINT NOT NULL COMMENT '关联SPU',
  quantity_requested INT NOT NULL COMMENT '请求生成数量',
  quantity_generated INT NOT NULL DEFAULT 0 COMMENT '已生成数量',
  quantity_printed INT NOT NULL DEFAULT 0 COMMENT '已打印数量',
  quantity_activated INT NOT NULL DEFAULT 0 COMMENT '已激活数量',
  manufacturer_node_id BIGINT NULL COMMENT '生产节点ID；B14 trace_node 落地前允许为空',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/GENERATING/GENERATED/PARTIAL_FAILED/FAILED/CANCELLED',
  operator_id BIGINT NULL COMMENT '创建操作人ID',
  operator_username VARCHAR(64) NULL COMMENT '创建操作人用户名',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_trace_assign_batch_no (batch_no),
  INDEX idx_trace_assign_batch_spu_id (spu_id),
  INDEX idx_trace_assign_batch_order_no (production_order_no),
  INDEX idx_trace_assign_batch_status (status),
  INDEX idx_trace_assign_batch_operator_id (operator_id),

  CONSTRAINT fk_trace_assign_batch_spu FOREIGN KEY (spu_id) REFERENCES base_part_spec(id) ON DELETE RESTRICT,
  CONSTRAINT fk_trace_assign_batch_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE SET NULL,
  CONSTRAINT ck_trace_assign_batch_quantity_requested CHECK (quantity_requested > 0),
  CONSTRAINT ck_trace_assign_batch_quantity_generated CHECK (quantity_generated >= 0 AND quantity_generated <= quantity_requested),
  CONSTRAINT ck_trace_assign_batch_quantity_printed CHECK (quantity_printed >= 0 AND quantity_printed <= quantity_requested),
  CONSTRAINT ck_trace_assign_batch_quantity_activated CHECK (quantity_activated >= 0 AND quantity_activated <= quantity_requested)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赋码批次表';

SELECT 'Trace assign batch table migration completed' AS result;
