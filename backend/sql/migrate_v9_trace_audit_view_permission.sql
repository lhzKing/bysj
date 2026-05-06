-- =====================================================
-- 迁移脚本 V9: 溯源审计完整视图权限
-- 日期: 2026-05-05
-- 说明:
--   1. 新增 trace:audit:view，专门控制 GET /api/traces/{traceCode}?view=audit
--   2. 为 SUPER_ADMIN / ADMIN 分配该权限；普通 trace:view 只能查看 effective 业务有效视图
-- =====================================================

INSERT INTO sys_permission (perm_code, perm_name, api_method, api_pattern, remark, create_time)
VALUES ('trace:audit:view', '溯源审计视图', NULL, NULL, '查看溯源审计完整历史；由详情接口 view=audit 参数做业务级权限校验', NOW())
ON DUPLICATE KEY UPDATE
    perm_name = VALUES(perm_name),
    api_method = VALUES(api_method),
    api_pattern = VALUES(api_pattern),
    remark = VALUES(remark);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code = 'trace:audit:view'
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

SELECT 'Trace audit view permission migration completed' AS result;
