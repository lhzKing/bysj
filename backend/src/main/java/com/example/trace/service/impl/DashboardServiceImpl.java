package com.example.trace.service.impl;

import com.example.trace.mapper.DashboardMapper;
import com.example.trace.service.DashboardService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Set<String> VALID_RANGES = Set.of("today", "7d", "30d", "180d", "all");
    private static final String DEFAULT_RANGE = "30d";

    private final DashboardMapper dashboardMapper;

    public DashboardServiceImpl(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    private String normalizeRange(String range) {
        if (range == null || range.isBlank() || !VALID_RANGES.contains(range)) {
            return DEFAULT_RANGE;
        }
        return range;
    }

    @Override
    public Map<String, Object> kpi(String range) {
        range = normalizeRange(range);
        Map<String, Object> result = dashboardMapper.selectKpi(range);
        if (result == null) {
            result = new HashMap<>();
        }
        result.put("range", range);
        return result;
    }

    @Override
    public List<Map<String, Object>> mapData(String range) {
        range = normalizeRange(range);
        return dashboardMapper.selectMapData(range);
    }

    @Override
    public List<Map<String, Object>> trend(String range) {
        range = normalizeRange(range);
        return dashboardMapper.selectTrend(range);
    }

    @Override
    public Map<String, Object> topology(String traceCode, String range) {
        range = normalizeRange(range);
        List<Map<String, Object>> edges = dashboardMapper.selectTopologyEdges(traceCode, range);

        Set<String> nodeNames = new LinkedHashSet<>();
        List<Map<String, Object>> links = new ArrayList<>();

        for (Map<String, Object> edge : edges) {
            String source = Objects.toString(edge.get("source"), "");
            String target = Objects.toString(edge.get("target"), "");
            if (source.isBlank() || target.isBlank()) {
                continue;
            }

            nodeNames.add(source);
            nodeNames.add(target);

            Map<String, Object> link = new HashMap<>();
            link.put("source", source);
            link.put("target", target);
            links.add(link);
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (String name : nodeNames) {
            Map<String, Object> node = new HashMap<>();
            node.put("name", name);
            node.put("symbolSize", 40);
            nodes.add(node);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("nodes", nodes);
        res.put("links", links);
        res.put("range", range);
        return res;
    }
}
