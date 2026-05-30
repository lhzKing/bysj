package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.util.ProvinceUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a plausible lifecycle log chain for a single demo trace code.
 *
 * <p>The core chain is:</p>
 * <pre>
 * INIT → PRINT_CODE → ACTIVATE_CODE → INBOUND
 * </pre>
 *
 * <p>That {@code INBOUND} is the finished-goods stock-in after activation. Demo
 * rows may then continue with a realistic logistics loop:</p>
 * <ul>
 *   <li>{@code OUTBOUND}: warehouse leaves stock and enters {@code IN_TRANSIT}</li>
 *   <li>{@code TRANSFER}: in-transit handoff / transit-location update, still {@code IN_TRANSIT}</li>
 *   <li>{@code DELIVER}: final customer sign-off, terminal {@code TRANSFERRED}</li>
 *   <li>{@code EXCEPTION_OPEN}: optional exception hold at the tail before final delivery</li>
 * </ul>
 *
 * <p>Hash + RSA signature are produced through {@link TraceLogFactory}, the same
 * factory the real scan path uses, so every chain entry passes
 * {@code GET /api/traces/{code}/verify}.</p>
 */
@Component
public class DemoChainBuilder {

    private final TraceLogFactory logFactory;

    public DemoChainBuilder(TraceLogFactory logFactory) {
        this.logFactory = logFactory;
    }

    /**
     * Outcome of {@link #buildChain}: the ordered log entries plus the tail
     * snapshot ready for persistence.
     */
    public record ChainResult(List<TraceLifecycleLog> logs, TraceSnapshot snapshot, String terminalStatus) {}

    /**
     * Build a complete lifecycle chain (logs + tail snapshot) for one demo trace.
     *
     * @param traceCode      single-item trace code
     * @param spuId          SPU id
     * @param factory        producing factory node (used for INIT/PRINT/ACTIVATE)
     * @param warehouse      finished-goods warehouse node (used for INBOUND/OUTBOUND)
     * @param logisticsNode  optional logistics hub used by OUTBOUND/TRANSFER branch
     * @param customer       optional customer node used by TRANSFER/DELIVER branch
     * @param producerOp     producer username (used as operator on producer actions)
     * @param warehouseOp    warehouse username (used on INBOUND/OUTBOUND)
     * @param logisticsOp    logistics username (used on TRANSFER/DELIVER)
     * @param startTime      INIT event time
     * @param rng            shared {@link Random} for reproducibility
     */
    public ChainResult buildChain(String traceCode, Long spuId,
                                  TraceNode factory, TraceNode warehouse,
                                  TraceNode logisticsNode, TraceNode customer,
                                  String producerOp, String warehouseOp, String logisticsOp,
                                  LocalDateTime startTime, Random rng) {
        List<TraceLifecycleLog> logs = new ArrayList<>(8);
        String prevHash = "GENESIS";
        LocalDateTime t = startTime.truncatedTo(ChronoUnit.SECONDS);

        // 1) INIT @ factory
        TraceLifecycleLog initLog = make(traceCode, spuId, ActionType.INIT,
                null, factory.getNodeName(),
                factory.getProvince(), factory.getCity(),
                null, t, prevHash, producerOp);
        logs.add(initLog);
        prevHash = initLog.getCurrentHash();

        // 2) PRINT_CODE @ factory
        t = t.plusMinutes(10 + rng.nextInt(31));
        TraceLifecycleLog printLog = make(traceCode, spuId, ActionType.PRINT_CODE,
                factory.getNodeName(), null,
                factory.getProvince(), factory.getCity(),
                "生产工作台打印标签", t, prevHash, producerOp);
        logs.add(printLog);
        prevHash = printLog.getCurrentHash();

        // 3) ACTIVATE_CODE @ factory
        t = t.plusMinutes(5 + rng.nextInt(21));
        TraceLifecycleLog activateLog = make(traceCode, spuId, ActionType.ACTIVATE_CODE,
                factory.getNodeName(), null,
                factory.getProvince(), factory.getCity(),
                "出厂前激活", t, prevHash, producerOp);
        logs.add(activateLog);
        prevHash = activateLog.getCurrentHash();

        // 4) INBOUND @ warehouse: finished goods enter stock before the first outbound.
        t = t.plusHours(1 + rng.nextInt(6));
        TraceLifecycleLog inboundLog = make(traceCode, spuId, ActionType.INBOUND,
                factory.getNodeName(), warehouse.getNodeName(),
                warehouse.getProvince(), warehouse.getCity(),
                "成品入库", t, prevHash, warehouseOp);
        logs.add(inboundLog);
        prevHash = inboundLog.getCurrentHash();

        String terminalStatus = "IN_STOCK";
        String terminalNode = warehouse.getNodeName();
        String terminalProvince = warehouse.getProvince();
        String terminalCity = warehouse.getCity();
        String terminalOwner = warehouse.getNodeName();

        // 5) 70% chance OUTBOUND warehouse → logistics/customer, entering transit.
        if (rng.nextDouble() < 0.7) {
            TraceNode outboundTarget = logisticsNode != null ? logisticsNode : customer;
            if (outboundTarget != null) {
                t = t.plusHours(2 + rng.nextInt(13));
                TraceLifecycleLog outboundLog = make(traceCode, spuId, ActionType.OUTBOUND,
                        warehouse.getNodeName(), outboundTarget.getNodeName(),
                        warehouse.getProvince(), warehouse.getCity(),
                        "出库发往" + outboundTarget.getNodeName(), t, prevHash, warehouseOp);
                logs.add(outboundLog);
                prevHash = outboundLog.getCurrentHash();
                terminalStatus = "IN_TRANSIT";
                terminalNode = outboundTarget.getNodeName();
                terminalProvince = outboundTarget.getProvince();
                terminalCity = outboundTarget.getCity();
                terminalOwner = outboundTarget.getNodeName();

                // 6) 60% chance TRANSFER current transit target → logistics/customer (still IN_TRANSIT).
                // If the current outbound target is already the customer, route the transfer
                // through logisticsNode when possible to avoid a same-node handoff in demo data.
                if (customer != null && rng.nextDouble() < 0.6) {
                    TraceNode transferTarget = customer;
                    if (logisticsNode != null && terminalNode.equals(customer.getNodeName())
                            && !logisticsNode.getNodeName().equals(terminalNode)) {
                        transferTarget = logisticsNode;
                    }
                    t = t.plusHours(6 + rng.nextInt(25));
                    TraceLifecycleLog transferLog = make(traceCode, spuId, ActionType.TRANSFER,
                            terminalNode, transferTarget.getNodeName(),
                            transferTarget.getProvince(), transferTarget.getCity(),
                            "运输中转至" + transferTarget.getNodeName(), t, prevHash, logisticsOp);
                    logs.add(transferLog);
                    prevHash = transferLog.getCurrentHash();
                    terminalStatus = "IN_TRANSIT";
                    terminalNode = transferTarget.getNodeName();
                    terminalProvince = transferTarget.getProvince();
                    terminalCity = transferTarget.getCity();
                    terminalOwner = transferTarget.getNodeName();
                }

                // 7) 55% chance final DELIVER at current transit target.
                if (customer != null && rng.nextDouble() < 0.55) {
                    String deliveryNode = terminalNode;
                    String deliveryProvince = terminalProvince;
                    String deliveryCity = terminalCity;
                    if (!customer.getNodeName().equals(terminalNode)) {
                        deliveryNode = customer.getNodeName();
                        deliveryProvince = customer.getProvince();
                        deliveryCity = customer.getCity();
                    }
                    t = t.plusHours(2 + rng.nextInt(9));
                    TraceLifecycleLog deliveryLog = make(traceCode, spuId, ActionType.DELIVER,
                            terminalNode, deliveryNode,
                            deliveryProvince, deliveryCity,
                            "最终签收/交付客户" + deliveryNode, t, prevHash, logisticsOp);
                    logs.add(deliveryLog);
                    prevHash = deliveryLog.getCurrentHash();
                    terminalStatus = "TRANSFERRED";
                    terminalNode = deliveryNode;
                    terminalProvince = deliveryProvince;
                    terminalCity = deliveryCity;
                    terminalOwner = deliveryNode;
                }
            }
        }

        // 8) 5% chance EXCEPTION_OPEN at tail for non-terminal goods.
        // TRANSFERRED is final delivery and must not be reopened into normal flow/exception flow.
        String restoreStatus = null;
        String restoreNode = null;
        String restoreOwner = null;
        if (!"TRANSFERRED".equals(terminalStatus) && rng.nextDouble() < 0.05) {
            // Capture pre-exception state so EXCEPTION_CLOSE can restore it
            // (real scan flow does the same in TraceScanTransactionService).
            restoreStatus = terminalStatus;
            restoreNode = terminalNode;
            restoreOwner = terminalOwner;

            t = t.plusHours(2 + rng.nextInt(9));
            TraceLifecycleLog exceptionLog = make(traceCode, spuId, ActionType.EXCEPTION_OPEN,
                    terminalNode, null,
                    terminalProvince, terminalCity,
                    "质检发现异常，待复核", t, prevHash, terminalOwner);
            logs.add(exceptionLog);
            terminalStatus = "EXCEPTION";
        }

        TraceLifecycleLog tail = logs.get(logs.size() - 1);
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode(traceCode);
        snapshot.setSpuId(spuId);
        snapshot.setCurrentStatus(terminalStatus);
        snapshot.setCurrentNode(terminalNode);
        snapshot.setCurrentOwner(terminalOwner);
        snapshot.setExceptionRestoreStatus(restoreStatus);
        snapshot.setExceptionRestoreNode(restoreNode);
        snapshot.setExceptionRestoreOwner(restoreOwner);
        snapshot.setProvince(ProvinceUtil.toFullName(terminalProvince));
        snapshot.setCity(terminalCity);
        snapshot.setLastEventTime(tail.getEventTime());
        snapshot.setLastHash(tail.getCurrentHash());
        snapshot.setVersion(0);
        // lastLogId is filled by TraceBatchCommitter after the log INSERT assigns its PK.

        return new ChainResult(logs, snapshot, terminalStatus);
    }

    private TraceLifecycleLog make(String traceCode, Long spuId, ActionType action,
                                   String fromNode, String toNode,
                                   String provinceShort, String city, String remark,
                                   LocalDateTime eventTime, String prevHash, String operator) {
        LocalDateTime truncatedEvent = eventTime.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime ingestTime = truncatedEvent; // deterministic: ingest == event
        String provinceFull = ProvinceUtil.toFullName(provinceShort);
        return logFactory.createLog(
                traceCode, spuId, action,
                fromNode, toNode,
                provinceFull, city,
                remark, truncatedEvent, ingestTime,
                prevHash, null, operator);
    }
}
