-- F17: 启停（软禁用）SPU 配件，避免直接删除已参与溯源的配件。
-- 现有 SPU 视为启用（enabled=1）。enabled=0 的配件仍可被读取/查询，但生产赋码与扫码流程应在
-- 业务层拒绝创建新批次（不在本迁移内引入硬约束，先靠前端筛选 + 业务文档约定）。

ALTER TABLE base_part_spec
  ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'enabled flag: 1=active, 0=disabled'
  AFTER remark;

-- 已存在的 SPU 全部置为启用，与历史行为一致。
UPDATE base_part_spec SET enabled = 1 WHERE enabled IS NULL;

CREATE INDEX idx_base_part_spec_enabled ON base_part_spec(enabled);
