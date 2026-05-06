-- =====================================================
-- 迁移脚本 V4: Admin 示例数据权限与危险接口隔离
-- 日期: 2026-05-02
-- 说明:
--   1. 新增 trace:data:generate / trace:data:clear 两个专用权限
--   2. 为 SUPER_ADMIN / ADMIN 分配上述权限，替代通过 role:manage 间接放行高危接口
-- =====================================================

INSERT INTO sys_permission (perm_code, perm_name, api_method, api_pattern, remark, create_time)
VALUES
    ('trace:data:generate', '生成示例数据', 'POST', '/api/admin/generate-sample-data', '生成演示/验证用溯源数据', NOW()),
    ('trace:data:clear', '清空溯源数据', 'DELETE', '/api/admin/clear-trace-data', '危险操作：清空全部溯源日志和快照', NOW())
ON DUPLICATE KEY UPDATE
    perm_name = VALUES(perm_name),
    api_method = VALUES(api_method),
    api_pattern = VALUES(api_pattern),
    remark = VALUES(remark);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code IN ('trace:data:generate', 'trace:data:clear')
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

SELECT 'Admin demo-data permissions migration completed' AS result;
