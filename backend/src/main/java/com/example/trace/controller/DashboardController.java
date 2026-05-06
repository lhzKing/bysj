package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 控制器 - RESTful API
 * 资源: dashboard (仪表盘统计数据)
 * 
 * 权限要求：dashboard:view（查看仪表盘）
 * 
 * 时间范围参数 range 可选值：
 * - today: 当天
 * - 7d: 近7天
 * - 30d: 近30天（默认）
 * - 180d: 近半年
 * - all: 全部
 */
@RestController
@RequestMapping("/api/dashboard")
@RequirePermission("dashboard:view")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * KPI 统计
     * GET /api/dashboard/kpi?range=30d
     */
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<Map<String, Object>>> kpi(
            @RequestParam(required = false, defaultValue = "30d") String range) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.kpi(range)));
    }

    /**
     * 地图数据（按省份聚合）
     * GET /api/dashboard/map?range=30d
     * 返回格式: { items: [], total: 0 }
     */
    @GetMapping("/map")
    public ResponseEntity<ApiResponse<Map<String, Object>>> map(
            @RequestParam(required = false, defaultValue = "30d") String range) {
        List<Map<String, Object>> items = dashboardService.mapData(range);
        Map<String, Object> result = new HashMap<>();
        result.put("items", items != null ? items : List.of());
        result.put("total", items != null ? items.size() : 0);
        result.put("range", range);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 趋势数据（按时间统计）
     * GET /api/dashboard/trend?range=30d
     * 返回格式: { items: [], total: 0 }
     */
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trend(
            @RequestParam(required = false, defaultValue = "30d") String range) {
        List<Map<String, Object>> items = dashboardService.trend(range);
        Map<String, Object> result = new HashMap<>();
        result.put("items", items != null ? items : List.of());
        result.put("total", items != null ? items.size() : 0);
        result.put("range", range);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 拓扑图数据
     * GET /api/dashboard/topology?trace_code=xxx&range=30d
     */
    @GetMapping("/topology")
    public ResponseEntity<ApiResponse<Map<String, Object>>> topology(
            @RequestParam(required = false, name = "trace_code") String traceCode,
            @RequestParam(required = false, defaultValue = "30d") String range) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.topology(traceCode, range)));
    }
}
