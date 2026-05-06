-- 更新省份名称：从简称转换为全称
-- 用于修复地图显示问题

SET NAMES utf8mb4;

-- 更新 trace_lifecycle_log 表
UPDATE trace_lifecycle_log SET province = '河北省' WHERE province = '河北';
UPDATE trace_lifecycle_log SET province = '山西省' WHERE province = '山西';
UPDATE trace_lifecycle_log SET province = '辽宁省' WHERE province = '辽宁';
UPDATE trace_lifecycle_log SET province = '吉林省' WHERE province = '吉林';
UPDATE trace_lifecycle_log SET province = '黑龙江省' WHERE province = '黑龙江';
UPDATE trace_lifecycle_log SET province = '江苏省' WHERE province = '江苏';
UPDATE trace_lifecycle_log SET province = '浙江省' WHERE province = '浙江';
UPDATE trace_lifecycle_log SET province = '安徽省' WHERE province = '安徽';
UPDATE trace_lifecycle_log SET province = '福建省' WHERE province = '福建';
UPDATE trace_lifecycle_log SET province = '江西省' WHERE province = '江西';
UPDATE trace_lifecycle_log SET province = '山东省' WHERE province = '山东';
UPDATE trace_lifecycle_log SET province = '河南省' WHERE province = '河南';
UPDATE trace_lifecycle_log SET province = '湖北省' WHERE province = '湖北';
UPDATE trace_lifecycle_log SET province = '湖南省' WHERE province = '湖南';
UPDATE trace_lifecycle_log SET province = '广东省' WHERE province = '广东';
UPDATE trace_lifecycle_log SET province = '海南省' WHERE province = '海南';
UPDATE trace_lifecycle_log SET province = '四川省' WHERE province = '四川';
UPDATE trace_lifecycle_log SET province = '贵州省' WHERE province = '贵州';
UPDATE trace_lifecycle_log SET province = '云南省' WHERE province = '云南';
UPDATE trace_lifecycle_log SET province = '陕西省' WHERE province = '陕西';
UPDATE trace_lifecycle_log SET province = '甘肃省' WHERE province = '甘肃';
UPDATE trace_lifecycle_log SET province = '青海省' WHERE province = '青海';
UPDATE trace_lifecycle_log SET province = '台湾省' WHERE province = '台湾';
UPDATE trace_lifecycle_log SET province = '内蒙古自治区' WHERE province = '内蒙古';
UPDATE trace_lifecycle_log SET province = '广西壮族自治区' WHERE province = '广西';
UPDATE trace_lifecycle_log SET province = '西藏自治区' WHERE province = '西藏';
UPDATE trace_lifecycle_log SET province = '宁夏回族自治区' WHERE province = '宁夏';
UPDATE trace_lifecycle_log SET province = '新疆维吾尔自治区' WHERE province = '新疆';
UPDATE trace_lifecycle_log SET province = '北京市' WHERE province = '北京';
UPDATE trace_lifecycle_log SET province = '天津市' WHERE province = '天津';
UPDATE trace_lifecycle_log SET province = '上海市' WHERE province = '上海';
UPDATE trace_lifecycle_log SET province = '重庆市' WHERE province = '重庆';
UPDATE trace_lifecycle_log SET province = '香港特别行政区' WHERE province = '香港';
UPDATE trace_lifecycle_log SET province = '澳门特别行政区' WHERE province = '澳门';

-- 更新 trace_snapshot 表
UPDATE trace_snapshot SET province = '河北省' WHERE province = '河北';
UPDATE trace_snapshot SET province = '山西省' WHERE province = '山西';
UPDATE trace_snapshot SET province = '辽宁省' WHERE province = '辽宁';
UPDATE trace_snapshot SET province = '吉林省' WHERE province = '吉林';
UPDATE trace_snapshot SET province = '黑龙江省' WHERE province = '黑龙江';
UPDATE trace_snapshot SET province = '江苏省' WHERE province = '江苏';
UPDATE trace_snapshot SET province = '浙江省' WHERE province = '浙江';
UPDATE trace_snapshot SET province = '安徽省' WHERE province = '安徽';
UPDATE trace_snapshot SET province = '福建省' WHERE province = '福建';
UPDATE trace_snapshot SET province = '江西省' WHERE province = '江西';
UPDATE trace_snapshot SET province = '山东省' WHERE province = '山东';
UPDATE trace_snapshot SET province = '河南省' WHERE province = '河南';
UPDATE trace_snapshot SET province = '湖北省' WHERE province = '湖北';
UPDATE trace_snapshot SET province = '湖南省' WHERE province = '湖南';
UPDATE trace_snapshot SET province = '广东省' WHERE province = '广东';
UPDATE trace_snapshot SET province = '海南省' WHERE province = '海南';
UPDATE trace_snapshot SET province = '四川省' WHERE province = '四川';
UPDATE trace_snapshot SET province = '贵州省' WHERE province = '贵州';
UPDATE trace_snapshot SET province = '云南省' WHERE province = '云南';
UPDATE trace_snapshot SET province = '陕西省' WHERE province = '陕西';
UPDATE trace_snapshot SET province = '甘肃省' WHERE province = '甘肃';
UPDATE trace_snapshot SET province = '青海省' WHERE province = '青海';
UPDATE trace_snapshot SET province = '台湾省' WHERE province = '台湾';
UPDATE trace_snapshot SET province = '内蒙古自治区' WHERE province = '内蒙古';
UPDATE trace_snapshot SET province = '广西壮族自治区' WHERE province = '广西';
UPDATE trace_snapshot SET province = '西藏自治区' WHERE province = '西藏';
UPDATE trace_snapshot SET province = '宁夏回族自治区' WHERE province = '宁夏';
UPDATE trace_snapshot SET province = '新疆维吾尔自治区' WHERE province = '新疆';
UPDATE trace_snapshot SET province = '北京市' WHERE province = '北京';
UPDATE trace_snapshot SET province = '天津市' WHERE province = '天津';
UPDATE trace_snapshot SET province = '上海市' WHERE province = '上海';
UPDATE trace_snapshot SET province = '重庆市' WHERE province = '重庆';
UPDATE trace_snapshot SET province = '香港特别行政区' WHERE province = '香港';
UPDATE trace_snapshot SET province = '澳门特别行政区' WHERE province = '澳门';

-- 验证更新结果
SELECT province, COUNT(*) as count FROM trace_lifecycle_log GROUP BY province ORDER BY count DESC;
