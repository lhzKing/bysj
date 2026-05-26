-- =====================================================
-- Migration V22: demo seed-master-data permission
-- Date: 2026-05-27
-- Scope:
--   1. Adds trace:data:seed-master permission for POST /api/admin/seed-master-data
--   2. Grants it to SUPER_ADMIN role only
-- Run order: after migrate_v21_part_enabled.sql
-- =====================================================

INSERT INTO sys_permission (perm_code, perm_name, api_method, api_pattern, remark)
VALUES
    ('trace:data:seed-master', '种入主数据', 'POST', '/api/admin/seed-master-data',
     '幂等地种入 trace_node / base_part_spec / demo 用户 / 用户节点绑定')
ON DUPLICATE KEY UPDATE
    perm_name = VALUES(perm_name),
    api_method = VALUES(api_method),
    api_pattern = VALUES(api_pattern),
    remark = VALUES(remark);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code = 'trace:data:seed-master'
WHERE r.role_code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

SELECT 'Migration v22 (demo seed-master-data permission) completed' AS result;
