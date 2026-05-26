package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceFlowTask;
import com.example.trace.entity.TraceFlowTaskScan;
import com.example.trace.entity.TraceNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DemoFlowTaskFactoryTest {

    private final DemoFlowTaskFactory factory = new DemoFlowTaskFactory();

    private final List<DemoFlowTaskFactory.ScanCandidate> candidates = candidatePool(120);
    private final List<TraceNode> warehouseNodes = List.of(
            node(10L, "WH-A", "WAREHOUSE", "江苏", "苏州市"),
            node(11L, "WH-B", "WAREHOUSE", "广东", "广州市"),
            node(12L, "WH-C", "WAREHOUSE", "重庆", "重庆市")
    );
    private final List<TraceNode> logisticsNodes = List.of(
            node(20L, "LG-A", "LOGISTICS", "上海", "上海市"),
            node(21L, "LG-B", "LOGISTICS", "四川", "成都市")
    );
    private final List<TraceNode> customerNodes = List.of(
            node(30L, "CUST-A", "CUSTOMER", "湖北", "武汉市")
    );
    private final List<DemoUserRef> warehouseUsers = List.of(new DemoUserRef(101L, "warehouse"));
    private final List<DemoUserRef> logisticsUsers = List.of(new DemoUserRef(102L, "logistics"));

    @Test
    void build_producesExactlyTotalTasksWithCorrectStatusDistribution() {
        List<DemoFlowTaskFactory.FlowTaskUnit> units = factory.build(
                candidates, warehouseNodes, logisticsNodes, customerNodes,
                warehouseUsers, logisticsUsers,
                LocalDateTime.of(2026, 4, 1, 9, 11, 0),
                new Random(20260527L)
        );

        assertThat(units).hasSize(DemoFlowTaskFactory.TOTAL_TASKS);

        Map<String, Long> byStatus = units.stream()
                .map(u -> u.task().getStatus())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        assertThat(byStatus.get("COMPLETED")).isEqualTo(25L);
        assertThat(byStatus.get("PROCESSING")).isEqualTo(15L);
        assertThat(byStatus.get("CREATED")).isEqualTo(10L);
        assertThat(byStatus.get("EXCEPTION")).isEqualTo(5L);
        assertThat(byStatus.get("CANCELLED")).isEqualTo(5L);
    }

    @Test
    void build_distinctSourceAndTargetNodesForEveryTask() {
        List<DemoFlowTaskFactory.FlowTaskUnit> units = factory.build(
                candidates, warehouseNodes, logisticsNodes, customerNodes,
                warehouseUsers, logisticsUsers,
                LocalDateTime.of(2026, 4, 1, 9, 11, 0),
                new Random(7L)
        );
        for (DemoFlowTaskFactory.FlowTaskUnit u : units) {
            assertThat(u.task().getSourceNodeId())
                    .as("task %s: source ≠ target", u.task().getTaskNo())
                    .isNotEqualTo(u.task().getTargetNodeId());
        }
    }

    @Test
    void build_completedTaskHasCompleteTimeAndCancelledHasCancelTime() {
        List<DemoFlowTaskFactory.FlowTaskUnit> units = factory.build(
                candidates, warehouseNodes, logisticsNodes, customerNodes,
                warehouseUsers, logisticsUsers,
                LocalDateTime.of(2026, 4, 1, 9, 11, 0),
                new Random(123L)
        );
        for (DemoFlowTaskFactory.FlowTaskUnit u : units) {
            TraceFlowTask task = u.task();
            switch (task.getStatus()) {
                case "COMPLETED" -> {
                    assertThat(task.getCompleteTime()).isNotNull();
                    assertThat(task.getCancelTime()).isNull();
                }
                case "CANCELLED" -> {
                    assertThat(task.getCancelTime()).isNotNull();
                    assertThat(task.getCompleteTime()).isNull();
                }
                case "CREATED", "PROCESSING", "EXCEPTION" -> {
                    assertThat(task.getCompleteTime()).isNull();
                    assertThat(task.getCancelTime()).isNull();
                }
                default -> {} // unreachable per status plan
            }
        }
    }

    @Test
    void build_discrepancyQuantityEqualsAbsoluteDifferenceWhenSet() {
        List<DemoFlowTaskFactory.FlowTaskUnit> units = factory.build(
                candidates, warehouseNodes, logisticsNodes, customerNodes,
                warehouseUsers, logisticsUsers,
                LocalDateTime.of(2026, 4, 1, 9, 11, 0),
                new Random(99L)
        );
        for (DemoFlowTaskFactory.FlowTaskUnit u : units) {
            TraceFlowTask task = u.task();
            if (!"NONE".equals(task.getDiscrepancyType())) {
                int abs = Math.abs(task.getExpectedQuantity() - task.getActualQuantity());
                assertThat(task.getDiscrepancyQuantity())
                        .as("task %s: discrepancy_quantity must equal |expected - actual|", task.getTaskNo())
                        .isEqualTo(abs);
            }
        }
    }

    @Test
    void build_createdAndCancelledTasksHaveNoScans_otherStatusesScanCountEqualsActualQuantity() {
        List<DemoFlowTaskFactory.FlowTaskUnit> units = factory.build(
                candidates, warehouseNodes, logisticsNodes, customerNodes,
                warehouseUsers, logisticsUsers,
                LocalDateTime.of(2026, 4, 1, 9, 11, 0),
                new Random(444L)
        );
        for (DemoFlowTaskFactory.FlowTaskUnit u : units) {
            TraceFlowTask task = u.task();
            List<TraceFlowTaskScan> scans = u.scans();
            switch (task.getStatus()) {
                case "CREATED", "CANCELLED" -> assertThat(scans).isEmpty();
                case "PROCESSING", "COMPLETED", "EXCEPTION" -> {
                    int expectedScanCount = Math.min(task.getActualQuantity(), candidates.size());
                    assertThat(scans).hasSize(expectedScanCount);
                }
                default -> {} // unreachable
            }
        }
    }

    @Test
    void build_scanActionTypeMatchesTaskTypeWithReceiveMappedToInbound() {
        List<DemoFlowTaskFactory.FlowTaskUnit> units = factory.build(
                candidates, warehouseNodes, logisticsNodes, customerNodes,
                warehouseUsers, logisticsUsers,
                LocalDateTime.of(2026, 4, 1, 9, 11, 0),
                new Random(20260527L)
        );
        for (DemoFlowTaskFactory.FlowTaskUnit u : units) {
            String taskType = u.task().getTaskType();
            String expectedAction = switch (taskType) {
                case "OUTBOUND" -> "OUTBOUND";
                case "INBOUND", "RECEIVE" -> "INBOUND";
                case "TRANSFER" -> "TRANSFER";
                default -> throw new IllegalStateException("unknown task type " + taskType);
            };
            for (TraceFlowTaskScan scan : u.scans()) {
                assertThat(scan.getActionType()).isEqualTo(expectedAction);
            }
        }
    }

    @Test
    void build_scanRowsWithinOneTaskHaveDistinctTraceCodes() {
        List<DemoFlowTaskFactory.FlowTaskUnit> units = factory.build(
                candidates, warehouseNodes, logisticsNodes, customerNodes,
                warehouseUsers, logisticsUsers,
                LocalDateTime.of(2026, 4, 1, 9, 11, 0),
                new Random(20260527L)
        );
        for (DemoFlowTaskFactory.FlowTaskUnit u : units) {
            Set<String> codes = new HashSet<>();
            for (TraceFlowTaskScan scan : u.scans()) {
                assertThat(codes.add(scan.getTraceCode()))
                        .as("task %s: trace_code %s appears twice", u.task().getTaskNo(), scan.getTraceCode())
                        .isTrue();
            }
        }
    }

    private static TraceNode node(Long id, String name, String type, String province, String city) {
        TraceNode n = new TraceNode();
        n.setId(id);
        n.setNodeCode("NODE-" + id);
        n.setNodeName(name);
        n.setNodeType(type);
        n.setProvince(province);
        n.setCity(city);
        return n;
    }

    private static List<DemoFlowTaskFactory.ScanCandidate> candidatePool(int n) {
        List<DemoFlowTaskFactory.ScanCandidate> list = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            list.add(new DemoFlowTaskFactory.ScanCandidate("TC-CAND-%04d".formatted(i)));
        }
        return list;
    }
}
