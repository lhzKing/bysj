-- =====================================================
-- 清理旧数据脚本
-- 用于删除安全增强前创建的溯源日志和快照
-- 保留零件基础数据（base_part_spec）
-- =====================================================

-- 1. 删除所有溯源生命周期日志
DELETE FROM trace_lifecycle_log;

-- 2. 删除所有溯源快照
DELETE FROM trace_snapshot;

-- 3. 重置自增ID（可选，保持日志ID从1开始）
ALTER TABLE trace_lifecycle_log AUTO_INCREMENT = 1;

-- 4. 验证清理结果
SELECT 'trace_lifecycle_log' AS table_name, COUNT(*) AS record_count FROM trace_lifecycle_log
UNION ALL
SELECT 'trace_snapshot' AS table_name, COUNT(*) AS record_count FROM trace_snapshot
UNION ALL
SELECT 'base_part_spec' AS table_name, COUNT(*) AS record_count FROM base_part_spec;

-- =====================================================
-- 清理完成后，按以下步骤测试：
-- 1. 重启后端服务
-- 2. 用 Postman 运行 "0. 准备工作 - 创建新溯源码"
-- 3. 运行 "1. 验链功能测试" 验证 Hash + 签名
-- =====================================================
