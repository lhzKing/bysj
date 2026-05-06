package com.example.trace.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    /**
     * KPI 统计
     * @param range 时间范围: today, 7d, 30d, 180d, all
     */
    Map<String, Object> selectKpi(@Param("range") String range);

    /**
     * 地图数据（按省份聚合）
     * @param range 时间范围: today, 7d, 30d, 180d, all
     */
    List<Map<String, Object>> selectMapData(@Param("range") String range);

    /**
     * 趋势数据
     * @param range 时间范围: today(按小时), 7d(按天), 30d/180d/all(按天)
     */
    List<Map<String, Object>> selectTrend(@Param("range") String range);

    /**
     * 拓扑图边数据
     * @param traceCode 溯源码（可选，不传则按时间范围查询）
     * @param range 时间范围
     */
    List<Map<String, Object>> selectTopologyEdges(@Param("traceCode") String traceCode, @Param("range") String range);
}
