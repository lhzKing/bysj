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
 * <p>The default chain is 5 steps:
 * {@code INIT → PRINT_CODE → ACTIVATE_CODE → OUTBOUND → INBOUND}.
 * Two probabilistic branches extend the chain:</p>
 * <ul>
 *   <li>60% chance: append {@code TRANSFER} (warehouse → logistics)</li>
 *   <li>after that, 55% chance: append another {@code TRANSFER} (logistics → customer)</li>
 *   <li>5% chance overall: append {@code EXCEPTION_OPEN} at the tail</li>
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
     * @param factory        producing factory node (used for INIT/PRINT/ACTIVATE/OUTBOUND)
     * @param warehouse      warehouse node receiving INBOUND
     * @param logisticsNode  optional logistics hub used by TRANSFER branch
     * @param customer       optional customer node used by final TRANSFER branch
     * @param producerOp     producer username (used as operator on producer actions)
     * @param warehouseOp    warehouse username (used on INBOUND)
     * @param logisticsOp    logistics username (used on TRANSFER)
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

        // 4) OUTBOUND factory → warehouse
        t = t.plusHours(2 + rng.nextInt(13));
        TraceLifecycleLog outboundLog = make(traceCode, spuId, ActionType.OUTBOUND,
                factory.getNodeName(), warehouse.getNodeName(),
                factory.getProvince(), factory.getCity(),
                "发往" + warehouse.getNodeName(), t, prevHash, producerOp);
        logs.add(outboundLog);
        prevHash = outboundLog.getCurrentHash();

        // 5) INBOUND @ warehouse
        t = t.plusHours(6 + rng.nextInt(37));
        TraceLifecycleLog inboundLog = make(traceCode, spuId, ActionType.INBOUND,
                factory.getNodeName(), warehouse.getNodeName(),
                warehouse.getProvince(), warehouse.getCity(),
                "到仓签收", t, prevHash, warehouseOp);
        logs.add(inboundLog);
        prevHash = inboundLog.getCurrentHash();

        String terminalStatus = "IN_STOCK";
        String terminalNode = warehouse.getNodeName();
        String terminalProvince = warehouse.getProvince();
        String terminalCity = warehouse.getCity();
        String terminalOwner = warehouseOp;

        // 6) 60% chance TRANSFER warehouse → logistics
        if (logisticsNode != null && rng.nextDouble() < 0.6) {
            t = t.plusHours(12 + rng.nextInt(49));
            TraceLifecycleLog transferLog = make(traceCode, spuId, ActionType.TRANSFER,
                    warehouse.getNodeName(), logisticsNode.getNodeName(),
                    logisticsNode.getProvince(), logisticsNode.getCity(),
                    "调拨至" + logisticsNode.getNodeName(), t, prevHash, logisticsOp);
            logs.add(transferLog);
            prevHash = transferLog.getCurrentHash();
            terminalStatus = "IN_TRANSIT";
            terminalNode = logisticsNode.getNodeName();
            terminalProvince = logisticsNode.getProvince();
            terminalCity = logisticsNode.getCity();
            terminalOwner = logisticsOp;

            // 7) 55% chance TRANSFER logistics → customer (final delivery)
            if (customer != null && rng.nextDouble() < 0.55) {
                t = t.plusHours(6 + rng.nextInt(25));
                TraceLifecycleLog deliveryLog = make(traceCode, spuId, ActionType.TRANSFER,
                        logisticsNode.getNodeName(), customer.getNodeName(),
                        customer.getProvince(), customer.getCity(),
                        "交付客户" + customer.getNodeName(), t, prevHash, logisticsOp);
                logs.add(deliveryLog);
                prevHash = deliveryLog.getCurrentHash();
                terminalStatus = "TRANSFERRED";
                terminalNode = customer.getNodeName();
                terminalProvince = customer.getProvince();
                terminalCity = customer.getCity();
                // owner stays logistics (last hand-off operator)
            }
        }

        // 8) 5% chance EXCEPTION_OPEN at tail
        if (rng.nextDouble() < 0.05) {
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
