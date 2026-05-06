-- ==================== 数据库迁移脚本 ====================
-- 版本: v2.0.0 - 安全增强版
-- 日期: 2026-01-18
-- 说明: 添加数字签名字段和乐观锁版本字段
-- 
-- 执行方式: mysql -u username -p trace_db < migrate_v2.sql
-- ========================================================

-- 1. 为 trace_lifecycle_log 表添加数字签名字段
ALTER TABLE trace_lifecycle_log 
ADD COLUMN signature VARCHAR(512) COMMENT 'RSA数字签名（Base64编码）' 
AFTER operator;

-- 2. 为 trace_snapshot 表添加乐观锁版本字段
ALTER TABLE trace_snapshot 
ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' 
AFTER last_hash;

-- 3. 为签名字段添加索引（可选，如果需要按签名查询）
-- CREATE INDEX idx_signature ON trace_lifecycle_log(signature(64));

-- 4. 验证迁移结果
SELECT 'trace_lifecycle_log 表结构:' AS info;
DESCRIBE trace_lifecycle_log;

SELECT 'trace_snapshot 表结构:' AS info;
DESCRIBE trace_snapshot;

-- 5. 输出迁移完成信息
SELECT '数据库迁移完成！' AS result, NOW() AS migrate_time;
