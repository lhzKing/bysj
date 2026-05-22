-- reseed_demo_v2.sql
-- 一次性清空所有 demo 流转数据 + 元器件 + 节点（保留 sys_user / sys_role / sys_role_permission），
-- 然后重新种入符合工业供应链场景的真实化主数据：
--   - 6 个 trace_node（北京工厂 + 苏州/广州仓 + 上海/成都转运 + 武汉客户）
--   - 15 个 base_part_spec（阀门 / 轴承 / 电机 / 传感器 / 管件 各 3）
--   - 3 条 trace_user_node_binding（producer↔BJ / warehouse↔SZ / logistics↔SH）
--
-- 跑完后再调用 POST /api/admin/generate-sample-data?count=50 让后端用 RSA 签名
-- 批量铺 50 条带哈希链的 trace（动作类型 INIT/INBOUND/OUTBOUND/TRANSFER 自然组合）。
--
-- 安全前提：sys_user / sys_role / sys_role_permission / sys_permission / signature_key 不动。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============= Cleanup =============
DELETE FROM trace_lifecycle_log;
DELETE FROM trace_snapshot;
DELETE FROM trace_code;
DELETE FROM trace_scan_idempotency;
DELETE FROM trace_assign_batch;
DELETE FROM trace_aggregation;
DELETE FROM trace_flow_task_scan;
DELETE FROM trace_flow_task;
DELETE FROM trace_user_node_binding;
DELETE FROM trace_node;
DELETE FROM base_part_spec;

-- 重置自增计数（让新数据 ID 从 1 开始，截图更整洁）
ALTER TABLE trace_lifecycle_log AUTO_INCREMENT = 1;
ALTER TABLE trace_snapshot AUTO_INCREMENT = 1;
ALTER TABLE trace_code AUTO_INCREMENT = 1;
ALTER TABLE trace_assign_batch AUTO_INCREMENT = 1;
ALTER TABLE trace_node AUTO_INCREMENT = 1;
ALTER TABLE base_part_spec AUTO_INCREMENT = 1;

-- ============= 6 trace_node =============
INSERT INTO trace_node (node_code, node_name, node_type, province, city, address) VALUES
  ('NODE-FACTORY-BJ',    '北京通用电气制造厂',  'FACTORY',   '北京', '北京市', '朝阳区将台路 5 号'),
  ('NODE-WAREHOUSE-SZ',  '苏州中央仓储',        'WAREHOUSE', '江苏', '苏州市', '工业园区星湖街 328 号'),
  ('NODE-WAREHOUSE-GZ',  '广州华南仓储',        'WAREHOUSE', '广东', '广州市', '番禺区市广路 168 号'),
  ('NODE-LOGISTICS-SH',  '上海顺丰转运中心',    'LOGISTICS', '上海', '上海市', '浦东新区周浦镇沪南公路'),
  ('NODE-LOGISTICS-CD',  '成都德邦转运中心',    'LOGISTICS', '四川', '成都市', '双流区西航港大道'),
  ('NODE-CUSTOMER-WH',   '武汉东风汽车整装厂',  'CUSTOMER',  '湖北', '武汉市', '武汉经济技术开发区车城大道');

-- ============= 15 base_part_spec =============
-- 包含 TraceDemoDataServiceImpl#PART_DEFINITIONS 写死的 5 个 part_code
-- (SPU-VALVE-002 / SPU-BEAR-001 / SPU-MOTOR-001 / SPU-SENS-001 / SPU-PIPE-001)
-- 后续 generate-sample-data 调用时不会重复插入。
INSERT INTO base_part_spec (part_code, part_name, part_type, model, manufacturer, unit, remark, enabled) VALUES
  -- 阀门类
  ('SPU-VALVE-001', '气动球阀',       '阀门类', 'V-2024001', '上海阀门厂',     '件', '工业气动球阀，DN50 PN16',   1),
  ('SPU-VALVE-002', '电动蝶阀',       '阀门类', 'V-2024002', '浙江阀门集团',   '件', '法兰式电动蝶阀，DN100',    1),
  ('SPU-VALVE-003', '安全阀',         '阀门类', 'V-2024003', '山东安全设备',   '件', '弹簧式安全阀，DN25',       1),
  -- 轴承类
  ('SPU-BEAR-001',  '深沟球轴承',     '轴承类', 'B-6205',    'SKF中国',        '件', '内径 25mm，外径 52mm',     1),
  ('SPU-BEAR-002',  '圆柱滚子轴承',   '轴承类', 'B-NU206',   '哈尔滨轴承',     '件', '内径 30mm，单列',          1),
  ('SPU-BEAR-003',  '调心滚子轴承',   '轴承类', 'B-22208',   '洛阳LYC',        '件', '内径 40mm，重载工况',      1),
  -- 电机类
  ('SPU-MOTOR-001', '三相异步电机',   '电机类', 'M-Y160M',   '卧龙电机',       '件', 'YE3 高效，11kW',           1),
  ('SPU-MOTOR-002', '永磁同步电机',   '电机类', 'M-PMSM150', '大洋电机',       '件', '15kW 伺服级',              1),
  ('SPU-MOTOR-003', '直流伺服电机',   '电机类', 'M-DC110',   '佳木斯电机',     '件', '110V 直流，2.4kW',         1),
  -- 传感器类
  ('SPU-SENS-001',  '温度传感器',     '传感器类','S-PT100',   'E+H中国',        '件', 'PT100 三线制',             1),
  ('SPU-SENS-002',  '压力传感器',     '传感器类','S-PR250',   '横河中国',       '件', '0-25MPa 4-20mA',           1),
  ('SPU-SENS-003',  '流量传感器',     '传感器类','S-FL80',    'SICK中国',       '件', '电磁式 DN80',              1),
  -- 管件类
  ('SPU-PIPE-001',  '无缝钢管',       '管件类', 'P-DN100',   '宝钢股份',       '件', '碳钢 Sch40，DN100',        1),
  ('SPU-PIPE-002',  '法兰盘',         '管件类', 'P-FL150',   '重庆法兰',       '件', 'PN16 平焊法兰 DN150',      1),
  ('SPU-PIPE-003',  '弯头',           '管件类', 'P-EL90',    '河北管件',       '件', '90 度无缝弯头 DN80',       1);

-- ============= 3 trace_user_node_binding =============
-- producer ↔ NODE-FACTORY-BJ；warehouse ↔ NODE-WAREHOUSE-SZ；logistics ↔ NODE-LOGISTICS-SH
INSERT INTO trace_user_node_binding (user_id, node_id)
SELECT u.id, n.id
FROM sys_user u, trace_node n
WHERE (u.username = 'producer'  AND n.node_code = 'NODE-FACTORY-BJ')
   OR (u.username = 'warehouse' AND n.node_code = 'NODE-WAREHOUSE-SZ')
   OR (u.username = 'logistics' AND n.node_code = 'NODE-LOGISTICS-SH');

SET FOREIGN_KEY_CHECKS = 1;
