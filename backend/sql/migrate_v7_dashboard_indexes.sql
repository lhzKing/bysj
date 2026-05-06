-- =====================================================
-- 迁移脚本 V7: Dashboard 性能索引（T-P1-03）
-- 日期: 2026-05-05
-- 说明:
--   1. 为 trace_snapshot.last_event_time 加索引——配合 DashboardMapper.selectKpi
--      把 DATE(last_event_time)=CURDATE() 重写为可走索引的范围过滤后生效。
--   2. 为 trace_lifecycle_log.action_type 加索引——支持 selectKpi 的
--      EXCEPTION SUM 和未来按 action_type 过滤的统计。
--
-- 安全性：
--   - 两个 CREATE INDEX 在已存在同名索引时 MySQL 5.7+ 会报错。本脚本通过先 DROP
--     再 CREATE 的方式实现幂等；DROP 在索引不存在时也会报错，因此先用
--     INFORMATION_SCHEMA 判断存在性。
--   - 不会回填/重写任何业务数据，只新增二级索引；CREATE INDEX 在大表上是
--     在线 DDL（InnoDB 默认 ALGORITHM=INPLACE），可在低峰期执行。
-- =====================================================

-- trace_snapshot.last_event_time
SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trace_snapshot'
      AND INDEX_NAME = 'idx_trace_snapshot_last_event_time'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_trace_snapshot_last_event_time ON trace_snapshot(last_event_time)',
    'SELECT ''idx_trace_snapshot_last_event_time already exists, skip'' AS skip_reason'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- trace_lifecycle_log.action_type
SET @idx_exists := (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trace_lifecycle_log'
      AND INDEX_NAME = 'idx_action_type'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_action_type ON trace_lifecycle_log(action_type)',
    'SELECT ''idx_action_type already exists, skip'' AS skip_reason'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Dashboard performance index migration v7 completed' AS result;
