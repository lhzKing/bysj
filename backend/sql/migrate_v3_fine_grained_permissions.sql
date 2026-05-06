-- =====================================================
-- 迁移脚本 V3: 细粒度溯源权限
-- 日期: 2026-01-21
-- 说明: 拆分 trace:scan 为更细粒度的入库/出库/流转权限
-- =====================================================

-- 1. 添加新的细粒度权限
-- 1. 添加新的细粒度权限（兼容现有表结构）
-- 注意：实际表 `sys_permission` 的列为
-- (perm_code, perm_name, api_method, api_pattern, remark, create_time)
-- 因此使用与之匹配的列名进行插入/更新
INSERT INTO sys_permission (perm_code, perm_name, api_method, api_pattern, remark, create_time)
VALUES 
    ('trace:inbound', '入库操作', 'POST', '/api/traces/*/events', '允许执行入库扫码操作', NOW()),
    ('trace:outbound', '出库操作', 'POST', '/api/traces/*/events', '允许执行出库扫码操作', NOW()),
    ('trace:transfer', '物流流转', 'POST', '/api/traces/*/events', '允许执行物流流转扫码操作', NOW())
ON DUPLICATE KEY UPDATE
    perm_name = VALUES(perm_name),
    api_method = VALUES(api_method),
    api_pattern = VALUES(api_pattern),
    remark = VALUES(remark);

-- 2. 为仓库管理员角色分配入库和出库权限（示例）
-- 请根据实际的角色ID进行调整
-- INSERT INTO sys_role_permission (role_id, permission_id)
-- SELECT 
--     (SELECT id FROM sys_role WHERE role_code = 'WAREHOUSE'),
--     id
-- FROM sys_permission 
-- WHERE perm_code IN ('trace:inbound', 'trace:outbound');

-- 3. 为物流角色分配流转权限（示例）
-- INSERT INTO sys_role_permission (role_id, permission_id)
-- SELECT 
--     (SELECT id FROM sys_role WHERE role_code = 'LOGISTICS'),
--     id
-- FROM sys_permission 
-- WHERE perm_code = 'trace:transfer';

-- 注意：
-- 1. trace:scan 权限保留，作为"超级扫码权限"，拥有此权限可执行所有扫码操作
-- 2. 细粒度权限(trace:inbound/outbound/transfer)只允许对应扫码动作
-- 3. 权限继承规则由代码层统一维护：trace:inbound/outbound/transfer 自动包含 trace:view，但不自动升级为 trace:scan

SELECT '细粒度权限迁移完成' AS result;
