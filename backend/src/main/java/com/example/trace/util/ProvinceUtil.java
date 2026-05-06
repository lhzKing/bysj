package com.example.trace.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 省份名称工具类
 * 用于处理省份简称和全称的转换
 */
public class ProvinceUtil {

    /**
     * 简称到全称的映射
     */
    private static final Map<String, String> SHORT_TO_FULL = new HashMap<>();

    /**
     * 全称到全称的映射（用于识别已经是全称的情况）
     */
    private static final Map<String, String> FULL_NAMES = new HashMap<>();

    static {
        // 23个省
        SHORT_TO_FULL.put("河北", "河北省");
        SHORT_TO_FULL.put("山西", "山西省");
        SHORT_TO_FULL.put("辽宁", "辽宁省");
        SHORT_TO_FULL.put("吉林", "吉林省");
        SHORT_TO_FULL.put("黑龙江", "黑龙江省");
        SHORT_TO_FULL.put("江苏", "江苏省");
        SHORT_TO_FULL.put("浙江", "浙江省");
        SHORT_TO_FULL.put("安徽", "安徽省");
        SHORT_TO_FULL.put("福建", "福建省");
        SHORT_TO_FULL.put("江西", "江西省");
        SHORT_TO_FULL.put("山东", "山东省");
        SHORT_TO_FULL.put("河南", "河南省");
        SHORT_TO_FULL.put("湖北", "湖北省");
        SHORT_TO_FULL.put("湖南", "湖南省");
        SHORT_TO_FULL.put("广东", "广东省");
        SHORT_TO_FULL.put("海南", "海南省");
        SHORT_TO_FULL.put("四川", "四川省");
        SHORT_TO_FULL.put("贵州", "贵州省");
        SHORT_TO_FULL.put("云南", "云南省");
        SHORT_TO_FULL.put("陕西", "陕西省");
        SHORT_TO_FULL.put("甘肃", "甘肃省");
        SHORT_TO_FULL.put("青海", "青海省");
        SHORT_TO_FULL.put("台湾", "台湾省");

        // 5个自治区
        SHORT_TO_FULL.put("内蒙古", "内蒙古自治区");
        SHORT_TO_FULL.put("广西", "广西壮族自治区");
        SHORT_TO_FULL.put("西藏", "西藏自治区");
        SHORT_TO_FULL.put("宁夏", "宁夏回族自治区");
        SHORT_TO_FULL.put("新疆", "新疆维吾尔自治区");

        // 4个直辖市
        SHORT_TO_FULL.put("北京", "北京市");
        SHORT_TO_FULL.put("天津", "天津市");
        SHORT_TO_FULL.put("上海", "上海市");
        SHORT_TO_FULL.put("重庆", "重庆市");

        // 2个特别行政区
        SHORT_TO_FULL.put("香港", "香港特别行政区");
        SHORT_TO_FULL.put("澳门", "澳门特别行政区");

        // 初始化全称映射
        for (String fullName : SHORT_TO_FULL.values()) {
            FULL_NAMES.put(fullName, fullName);
        }
    }

    /**
     * 将省份简称转换为全称
     * 如果输入已经是全称，则直接返回
     * 如果无法识别，则返回原始输入
     *
     * @param province 省份名称（简称或全称）
     * @return 省份全称
     */
    public static String toFullName(String province) {
        if (province == null || province.isBlank()) {
            return province;
        }

        String trimmed = province.trim();

        // 如果已经是全称，直接返回
        if (FULL_NAMES.containsKey(trimmed)) {
            return trimmed;
        }

        // 尝试转换简称
        String fullName = SHORT_TO_FULL.get(trimmed);
        return fullName != null ? fullName : trimmed;
    }

    /**
     * 判断是否为有效的省份名称（简称或全称）
     *
     * @param province 省份名称
     * @return 是否有效
     */
    public static boolean isValidProvince(String province) {
        if (province == null || province.isBlank()) {
            return false;
        }
        String trimmed = province.trim();
        return SHORT_TO_FULL.containsKey(trimmed) || FULL_NAMES.containsKey(trimmed);
    }
}
