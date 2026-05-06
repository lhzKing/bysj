-- ============================================================
-- V3 角色权限分配脚本 - 细粒度溯源权限
-- 功能：为WAREHOUSE和LOGISTICS角色分配新的细粒度权限
-- 执行时机：在 migrate_v3_fine_grained_permissions.sql 之后运行
-- ============================================================

-- 查询当前权限ID（用于后续分配）
SELECT id, perm_code, perm_name FROM sys_permission
WHERE perm_code IN ('trace:inbound', 'trace:outbound', 'trace:transfer');

-- 查询角色ID
SELECT id, role_code, role_name FROM sys_role
WHERE role_code IN ('WAREHOUSE', 'LOGISTICS');

-- ============================================================
-- 仓库人员(WAREHOUSE)权限分配
-- 可以：入库(INBOUND)、出库(OUTBOUND)
-- 不可以：流转(TRANSFER) - 这是物流人员的权限
-- ============================================================

-- 删除旧的 trace:scan 权限（如果存在）
DELETE FROM sys_role_permission 
WHERE role_id = (SELECT id FROM sys_role WHERE role_code = 'WAREHOUSE')
AND permission_id = (SELECT id FROM sys_permission WHERE perm_code = 'trace:scan');

-- 分配 trace:inbound 权限给WAREHOUSE
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 
    (SELECT id FROM sys_role WHERE role_code = 'WAREHOUSE'),
    (SELECT id FROM sys_permission WHERE perm_code = 'trace:inbound')
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission 
    WHERE role_id = (SELECT id FROM sys_role WHERE role_code = 'WAREHOUSE')
    AND permission_id = (SELECT id FROM sys_permission WHERE perm_code = 'trace:inbound')
);

-- 分配 trace:outbound 权限给WAREHOUSE
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 
    (SELECT id FROM sys_role WHERE role_code = 'WAREHOUSE'),
    (SELECT id FROM sys_permission WHERE perm_code = 'trace:outbound')
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission 
    WHERE role_id = (SELECT id FROM sys_role WHERE role_code = 'WAREHOUSE')
    AND permission_id = (SELECT id FROM sys_permission WHERE perm_code = 'trace:outbound')
);

-- ============================================================
-- 物流人员(LOGISTICS)权限分配
-- 可以：流转(TRANSFER)
-- 不可以：入库(INBOUND)、出库(OUTBOUND) - 这些是仓库人员的权限
-- ============================================================

-- 删除旧的 trace:scan 权限（如果存在）
DELETE FROM sys_role_permission 
WHERE role_id = (SELECT id FROM sys_role WHERE role_code = 'LOGISTICS')
AND permission_id = (SELECT id FROM sys_permission WHERE perm_code = 'trace:scan');

-- 分配 trace:transfer 权限给LOGISTICS
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 
    (SELECT id FROM sys_role WHERE role_code = 'LOGISTICS'),
    (SELECT id FROM sys_permission WHERE perm_code = 'trace:transfer')
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission 
    WHERE role_id = (SELECT id FROM sys_role WHERE role_code = 'LOGISTICS')
    AND permission_id = (SELECT id FROM sys_permission WHERE perm_code = 'trace:transfer')
);

-- ============================================================
-- 验证分配结果
-- ============================================================

-- 查看WAREHOUSE角色的权限
SELECT 
    r.role_code AS 角色编码,
    r.role_name AS 角色名称,
    p.perm_code AS 权限编码,
    p.perm_name AS 权限名称,
    p.api_method AS HTTP方法,
    p.api_pattern AS API路径
FROM sys_role r
JOIN sys_role_permission rp ON r.id = rp.role_id
JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.role_code = 'WAREHOUSE'
AND p.perm_code LIKE 'trace:%'
ORDER BY p.perm_code;

-- 查看LOGISTICS角色的权限
SELECT 
    r.role_code AS 角色编码,
    r.role_name AS 角色名称,
    p.perm_code AS 权限编码,
    p.perm_name AS 权限名称,
    p.api_method AS HTTP方法,
    p.api_pattern AS API路径
FROM sys_role r
JOIN sys_role_permission rp ON r.id = rp.role_id
JOIN sys_permission p ON rp.permission_id = p.id
WHERE r.role_code = 'LOGISTICS'
AND p.perm_code LIKE 'trace:%'
ORDER BY p.perm_code;

-- 预期结果：
-- WAREHOUSE 应该有 trace:inbound 和 trace:outbound
-- LOGISTICS 应该有 trace:transfer
