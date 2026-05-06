package com.example.trace.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    /**
     * KPI 统计
     * @param range 时间范围: today, 7d, 30d, 180d, all
     */
    Map<String, Object> kpi(String range);

    /**
     * 地图数据（按省份聚合）
     * @param range 时间范围
     */
    List<Map<String, Object>> mapData(String range);

    /**
     * 趋势数据
     * @param range 时间范围
     */
    List<Map<String, Object>> trend(String range);

    /**
     * 拓扑图数据
     * @param traceCode 溯源码（可选）
     * @param range 时间范围
     */
    Map<String, Object> topology(String traceCode, String range);
}
