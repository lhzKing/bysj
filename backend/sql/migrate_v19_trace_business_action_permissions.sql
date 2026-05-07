-- B29 trace business-action permissions
-- Adds explicit permissions for assignment batches, label print/activation,
-- warehouse/logistics task actions, and exception handling while keeping the
-- legacy trace:create / trace:scan permissions as compatibility super-perms.

INSERT INTO sys_permission (perm_code, perm_name, api_method, api_pattern, remark)
VALUES
    ('trace:batch:create', '创建赋码批次', 'POST', '/api/traces', '创建赋码批次并生成单品溯源码'),
    ('trace:code:print', '打印/重打/作废标签', NULL, NULL, '打印、重打或作废单品码标签；由控制器注解校验多个业务入口'),
    ('trace:code:activate', '激活单品码', 'POST', '/api/trace-codes/*/activate', '贴码后扫码激活或复核单品码'),
    ('trace:task:create', '创建流转任务', 'POST', '/api/trace-flow-tasks', '创建仓库/物流发货、入库或接收任务'),
    ('trace:task:scan', '任务内扫码', 'POST', '/api/trace-flow-tasks/*/scan', '在流转任务内扫描单品码、箱码或托盘码'),
    ('trace:task:complete', '完成流转任务', 'POST', '/api/trace-flow-tasks/*/complete', '完成任务并处理少扫/多扫差异'),
    ('trace:exception:handle', '处理溯源异常', NULL, NULL, '上报或处理溯源生命周期异常；由动作权限策略校验')
ON DUPLICATE KEY UPDATE
    perm_name = VALUES(perm_name),
    api_method = VALUES(api_method),
    api_pattern = VALUES(api_pattern),
    remark = VALUES(remark);

-- SUPER_ADMIN / ADMIN: grant all newly introduced permissions.
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code IN (
    'trace:batch:create',
    'trace:code:print',
    'trace:code:activate',
    'trace:task:create',
    'trace:task:scan',
    'trace:task:complete',
    'trace:exception:handle'
)
WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN')
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

-- PRODUCER: assignment batch + label operations + activation + exception reporting.
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code IN (
    'trace:batch:create',
    'trace:code:print',
    'trace:code:activate',
    'trace:exception:handle'
)
WHERE r.role_code = 'PRODUCER'
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

-- WAREHOUSE: create/scan/complete warehouse tasks and report exceptions.
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code IN (
    'trace:task:create',
    'trace:task:scan',
    'trace:task:complete',
    'trace:exception:handle'
)
WHERE r.role_code = 'WAREHOUSE'
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

-- LOGISTICS: handle logistics task scanning/completion and exception reporting.
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code IN (
    'trace:task:create',
    'trace:task:scan',
    'trace:task:complete',
    'trace:exception:handle'
)
WHERE r.role_code = 'LOGISTICS'
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
