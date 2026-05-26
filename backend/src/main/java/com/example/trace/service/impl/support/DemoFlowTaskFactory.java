package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceFlowTask;
import com.example.trace.entity.TraceFlowTaskScan;
import com.example.trace.entity.TraceNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Demo flow-task factory.
 *
 * <p>Produces a fixed batch of 60 {@link TraceFlowTask} rows distributed across
 * the five legal statuses and four task types, plus their {@link TraceFlowTaskScan}
 * details:</p>
 *
 * <pre>
 *   status:  COMPLETED 25 / PROCESSING 15 / CREATED 10 / EXCEPTION 5 / CANCELLED 5
 *   type:    OUTBOUND  / INBOUND / TRANSFER / RECEIVE  (round-robin)
 * </pre>
 *
 * <p>Business rules enforced:</p>
 * <ul>
 *   <li>{@code source_node_id ≠ target_node_id} (CHECK constraint)</li>
 *   <li>{@code RECEIVE} task_type maps to {@code INBOUND} scan action_type because
 *       {@code trace_flow_task_scan.action_type} only accepts OUTBOUND/INBOUND/TRANSFER</li>
 *   <li>CREATED / CANCELLED tasks have zero scans; PROCESSING / COMPLETED / EXCEPTION
 *       tasks have scan count equal to {@code actual_quantity}</li>
 *   <li>{@code discrepancy_quantity} = |expected − actual| when discrepancy_type ≠ NONE</li>
 *   <li>COMPLETED tasks get {@code complete_time}; CANCELLED tasks get {@code cancel_time}</li>
 * </ul>
 */
@Component
public class DemoFlowTaskFactory {

    public static final int TOTAL_TASKS = 60;

    private static final String[] TYPES_ROUND_ROBIN = { "OUTBOUND", "INBOUND", "TRANSFER", "RECEIVE" };

    /**
     * One assembled flow task plus its (possibly empty) list of scan details.
     * The scan rows reference the task by index — TraceBatchCommitter must
     * back-fill {@code scan.taskId} after the task INSERT assigns its PK.
     */
    public record FlowTaskUnit(TraceFlowTask task, List<TraceFlowTaskScan> scans) {}

    /** Eligible source trace_code for task scans (one that's IN_STOCK or IN_TRANSIT). */
    public record ScanCandidate(String traceCode) {}

    /**
     * @param scanCandidates  trace_code pool the scans pick from (no duplication
     *                        across scans within a single task)
     * @param warehouseNodes  warehouse-type nodes (≥ 1 required)
     * @param logisticsNodes  logistics-type nodes (≥ 1 required)
     * @param customerNodes   customer-type nodes (≥ 1 required for RECEIVE)
     * @param warehouseUsers  warehouse demo users (used as task.create_by)
     * @param logisticsUsers  logistics demo users (used as task.create_by)
     * @param baseTime        timeline base; tasks are spaced out around it
     * @param rng             shared RNG
     */
    public List<FlowTaskUnit> build(List<ScanCandidate> scanCandidates,
                                    List<TraceNode> warehouseNodes,
                                    List<TraceNode> logisticsNodes,
                                    List<TraceNode> customerNodes,
                                    List<DemoUserRef> warehouseUsers,
                                    List<DemoUserRef> logisticsUsers,
                                    LocalDateTime baseTime,
                                    Random rng) {
        if (warehouseNodes.isEmpty() || logisticsNodes.isEmpty() || customerNodes.isEmpty()) {
            throw new IllegalArgumentException("flow-task factory needs warehouse/logistics/customer nodes");
        }
        List<String> statusPlan = buildStatusPlan();
        Collections.shuffle(statusPlan, rng);

        List<FlowTaskUnit> units = new ArrayList<>(TOTAL_TASKS);
        for (int idx = 0; idx < TOTAL_TASKS; idx++) {
            String status = statusPlan.get(idx);
            String taskType = TYPES_ROUND_ROBIN[idx % TYPES_ROUND_ROBIN.length];

            TraceNode source;
            TraceNode target;
            switch (taskType) {
                case "OUTBOUND" -> {
                    source = pick(warehouseNodes, rng);
                    target = pick(logisticsNodes, rng);
                }
                case "INBOUND" -> {
                    source = pick(logisticsNodes, rng);
                    target = pick(warehouseNodes, rng);
                }
                case "TRANSFER" -> {
                    source = pick(warehouseNodes, rng);
                    target = pickDifferent(warehouseNodes, source, rng);
                }
                case "RECEIVE" -> {
                    source = pick(logisticsNodes, rng);
                    target = pick(customerNodes, rng);
                }
                default -> throw new IllegalStateException("unreachable task type: " + taskType);
            }
            if (source.getId().equals(target.getId())) {
                // last-resort guard against CHECK ck_trace_flow_task_distinct_nodes; rare since picks above already enforce distinct nodes
                target = pickDifferent(warehouseNodes, source, rng);
            }

            int expectedQty = 8 + rng.nextInt(23); // 8..30
            DemoUserRef creator = pickCreator(taskType, warehouseUsers, logisticsUsers, rng);

            int actualQty;
            String discrepancyType = "NONE";
            int discrepancyQty = 0;
            String discrepancyReason = null;
            LocalDateTime discrepancyTime = null;
            LocalDateTime completeTime = null;
            LocalDateTime cancelTime = null;
            LocalDateTime createTime = baseTime.plusDays(idx / 6L).plusHours(idx % 12L).plusMinutes(11)
                    .truncatedTo(ChronoUnit.SECONDS);

            switch (status) {
                case "CREATED" -> actualQty = 0;
                case "PROCESSING" -> actualQty = 1 + rng.nextInt(Math.max(1, expectedQty - 1));
                case "COMPLETED" -> {
                    actualQty = expectedQty;
                    if (rng.nextDouble() < 0.3) {
                        if (rng.nextBoolean()) {
                            actualQty = Math.max(1, expectedQty - 1 - rng.nextInt(3));
                            discrepancyType = "SHORTAGE";
                        } else {
                            actualQty = expectedQty + 1 + rng.nextInt(3);
                            discrepancyType = "OVERAGE";
                        }
                        discrepancyQty = Math.abs(expectedQty - actualQty);
                        discrepancyReason = "现场扫描数量与预期不一致，已记录";
                        discrepancyTime = createTime.plusHours(4);
                    }
                    completeTime = createTime.plusHours(8 + rng.nextInt(25));
                }
                case "CANCELLED" -> {
                    actualQty = 0;
                    cancelTime = createTime.plusHours(1 + rng.nextInt(6));
                }
                case "EXCEPTION" -> {
                    actualQty = rng.nextInt(expectedQty + 1);
                    if (actualQty < expectedQty) {
                        discrepancyType = "SHORTAGE";
                        discrepancyQty = expectedQty - actualQty;
                    }
                    discrepancyReason = "现场发现单据异常，挂起处理";
                    discrepancyTime = createTime.plusHours(2);
                }
                default -> throw new IllegalStateException("unreachable status: " + status);
            }

            TraceFlowTask task = new TraceFlowTask();
            task.setTaskNo(String.format("TASK-EXT-%04d-%s", idx + 1, taskType));
            task.setTaskType(taskType);
            task.setSourceNodeId(source.getId());
            task.setTargetNodeId(target.getId());
            task.setExpectedQuantity(expectedQty);
            task.setActualQuantity(actualQty);
            task.setStatus(status);
            task.setCreateBy(creator.id());
            task.setCreateByUsername(creator.username());
            task.setCompleteTime(completeTime);
            task.setCancelTime(cancelTime);
            task.setDiscrepancyType(discrepancyType);
            task.setDiscrepancyQuantity(discrepancyQty);
            task.setDiscrepancyReason(discrepancyReason);
            task.setDiscrepancyTime(discrepancyTime);
            task.setRemark("由 generate-sample-data 接口批量生成");

            List<TraceFlowTaskScan> scans = buildScans(taskType, status, actualQty,
                    scanCandidates, creator, createTime, rng);
            units.add(new FlowTaskUnit(task, scans));
        }
        return units;
    }

    private List<TraceFlowTaskScan> buildScans(String taskType, String status, int actualQty,
                                               List<ScanCandidate> scanCandidates,
                                               DemoUserRef creator, LocalDateTime createTime,
                                               Random rng) {
        if (actualQty <= 0) {
            return List.of();
        }
        if (!(status.equals("PROCESSING") || status.equals("COMPLETED") || status.equals("EXCEPTION"))) {
            return List.of();
        }
        int picks = Math.min(actualQty, scanCandidates.size());
        if (picks <= 0) {
            return List.of();
        }
        List<ScanCandidate> pool = new ArrayList<>(scanCandidates);
        Collections.shuffle(pool, rng);
        List<ScanCandidate> picked = pool.subList(0, picks);

        String scanAction = mapTaskTypeToScanAction(taskType);
        List<TraceFlowTaskScan> scans = new ArrayList<>(picks);
        LocalDateTime scanTime = createTime.plusMinutes(30).truncatedTo(ChronoUnit.SECONDS);
        for (int j = 0; j < picked.size(); j++) {
            ScanCandidate c = picked.get(j);
            TraceFlowTaskScan scan = new TraceFlowTaskScan();
            // taskId is back-filled by TraceBatchCommitter after task INSERT
            scan.setTraceCode(c.traceCode());
            scan.setActionType(scanAction);
            scan.setCounted(true);
            scan.setOperatorUserId(creator.id());
            scan.setOperatorUsername(creator.username());
            scan.setIdempotencyKey("flow-task-%s-%s".formatted(c.traceCode(), scanAction));
            scan.setScanTime(scanTime.plusMinutes(2L * j));
            scan.setDuplicateCount(0);
            scans.add(scan);
        }
        return scans;
    }

    private static String mapTaskTypeToScanAction(String taskType) {
        return switch (taskType) {
            case "OUTBOUND" -> "OUTBOUND";
            case "INBOUND", "RECEIVE" -> "INBOUND";
            case "TRANSFER" -> "TRANSFER";
            default -> throw new IllegalArgumentException("unsupported task type: " + taskType);
        };
    }

    private static DemoUserRef pickCreator(String taskType,
                                           List<DemoUserRef> warehouseUsers,
                                           List<DemoUserRef> logisticsUsers,
                                           Random rng) {
        return switch (taskType) {
            case "OUTBOUND", "INBOUND", "TRANSFER" -> pick(warehouseUsers.isEmpty() ? logisticsUsers : warehouseUsers, rng);
            case "RECEIVE" -> pick(logisticsUsers.isEmpty() ? warehouseUsers : logisticsUsers, rng);
            default -> throw new IllegalArgumentException("unsupported task type: " + taskType);
        };
    }

    private static <T> T pick(List<T> pool, Random rng) {
        return pool.get(rng.nextInt(pool.size()));
    }

    private static TraceNode pickDifferent(List<TraceNode> pool, TraceNode exclude, Random rng) {
        if (pool.size() == 1) {
            // Pool has only one node — caller must guarantee at least 2 nodes for TRANSFER
            return pool.get(0);
        }
        TraceNode picked;
        do {
            picked = pool.get(rng.nextInt(pool.size()));
        } while (picked.getId().equals(exclude.getId()));
        return picked;
    }

    private static List<String> buildStatusPlan() {
        List<String> plan = new ArrayList<>(TOTAL_TASKS);
        for (int i = 0; i < 25; i++) plan.add("COMPLETED");
        for (int i = 0; i < 15; i++) plan.add("PROCESSING");
        for (int i = 0; i < 10; i++) plan.add("CREATED");
        for (int i = 0; i < 5; i++)  plan.add("EXCEPTION");
        for (int i = 0; i < 5; i++)  plan.add("CANCELLED");
        return plan;
    }
}
