package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceAggregation;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.util.HashUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Demo aggregation factory.
 *
 * <p>Builds:</p>
 * <ul>
 *   <li>24 carton-level groupings (parent_code = {@code CARTON-EXT-…}) each with
 *       10–15 child trace codes, relation_type = {@code CARTON}</li>
 *   <li>6 pallet-level groupings (parent_code = {@code PALLET-EXT-…}) each
 *       grouping 3 cartons, relation_type = {@code PALLET}</li>
 * </ul>
 *
 * <p>For every aggregation row the factory ALSO appends a corresponding
 * lifecycle log to the affected trace chain:</p>
 * <ul>
 *   <li>{@link ActionType#PACK PACK} for every CARTON binding (one per child
 *       trace_code).</li>
 *   <li>{@link ActionType#PALLETIZE PALLETIZE} for every (PALLET, carton)
 *       binding — expanded to one log per single-item trace_code that lives
 *       inside the carton, matching the real
 *       {@code TraceAggregationServiceImpl.resolveMutableChildSnapshots}
 *       behaviour.</li>
 * </ul>
 *
 * <p>Each appended log is hash-chained off the trace's current
 * {@code lastHash} (via the shared {@link TraceLogFactory}), and the trace
 * snapshot's {@code lastHash} / {@code lastEventTime} are advanced in place so
 * subsequent appends and the final {@code TraceBatchCommitter} commit see a
 * consistent chain. Parent codes use {@code CARTON-} / {@code PALLET-}
 * prefixes so a follow-up {@code TraceAggregationServiceImpl.bindChild} call
 * (or its server-side equivalents) would treat them as parent codes rather
 * than single-item trace codes.</p>
 *
 * <p>Each row sets {@code active=1}, {@code bind_time} on a deterministic
 * timeline derived from {@code base_time}. {@code active_marker} is a STORED
 * GENERATED column in MySQL and is NOT written — it's omitted from the entity
 * so MyBatis-Plus doesn't try to assign it.</p>
 */
@Component
public class DemoAggregationFactory {

    public static final int TARGET_CARTONS = 24;
    public static final int TARGET_PALLETS = 6;
    public static final int CARTON_SIZE_MIN = 10;
    public static final int CARTON_SIZE_MAX = 15;
    public static final int CARTONS_PER_PALLET = 3;

    private static final String DEMO_REMARK = "由 generate-sample-data 接口批量生成";

    private final TraceLogFactory logFactory;

    public DemoAggregationFactory(TraceLogFactory logFactory) {
        this.logFactory = logFactory;
    }

    /**
     * @param aggregatableTraceCodes pool of trace codes eligible for packing
     *                               (status IN_STOCK / IN_TRANSIT)
     * @param chainsByTrace          map of trace_code → in-memory chain result;
     *                               the factory mutates {@code chain.logs()} and
     *                               {@code chain.snapshot()} in place to append
     *                               PACK / PALLETIZE entries. Pass an empty
     *                               map to suppress log generation (test
     *                               scenarios that only care about row shape).
     * @param createdBy              demo user that owns the binding action
     * @param baseTime               base timeline (cartons start at +15 days, pallets at +18 days)
     * @param rng                    shared RNG
     */
    public List<TraceAggregation> build(List<String> aggregatableTraceCodes,
                                        Map<String, DemoChainBuilder.ChainResult> chainsByTrace,
                                        DemoUserRef createdBy,
                                        LocalDateTime baseTime, Random rng) {
        List<TraceAggregation> rows = new ArrayList<>();
        if (aggregatableTraceCodes.isEmpty()) {
            return rows;
        }
        Map<String, DemoChainBuilder.ChainResult> safeChains =
                chainsByTrace == null ? Map.of() : chainsByTrace;

        List<String> pool = new ArrayList<>(aggregatableTraceCodes);
        Collections.shuffle(pool, rng);

        // CARTON pass: each carton groups 10-15 single-item trace codes; each
        // child gets a PACK lifecycle log chained off its current lastHash.
        Map<String, List<String>> cartonToChildren = new LinkedHashMap<>();
        int cursor = 0;
        for (int c = 0; c < TARGET_CARTONS && cursor < pool.size(); c++) {
            int size = CARTON_SIZE_MIN + rng.nextInt(CARTON_SIZE_MAX - CARTON_SIZE_MIN + 1);
            int end = Math.min(cursor + size, pool.size());
            if (end - cursor < CARTON_SIZE_MIN && !cartonToChildren.isEmpty()) {
                // Don't produce a degenerate small carton; stop here.
                break;
            }
            String cartonCode = "CARTON-EXT-%03d-%s".formatted(c + 1, shortUuid());
            LocalDateTime bindTime = baseTime.plusDays(15L).plusHours(c)
                    .withSecond(33).withNano(0).truncatedTo(ChronoUnit.SECONDS);
            List<String> children = new ArrayList<>(end - cursor);
            for (int i = cursor; i < end; i++) {
                String traceCode = pool.get(i);
                rows.add(buildRow(cartonCode, traceCode, "CARTON", createdBy, bindTime));
                children.add(traceCode);
                appendAggregationLog(traceCode, ActionType.PACK, cartonCode,
                        bindTime, createdBy, safeChains);
            }
            cartonToChildren.put(cartonCode, children);
            cursor = end;
        }

        // PALLET pass: every pallet bundles 3 cartons. PALLETIZE expands to
        // one log per single-item child under the carton (mirrors the real
        // service's resolveMutableChildSnapshots recursion).
        List<String> cartonCodes = new ArrayList<>(cartonToChildren.keySet());
        for (int p = 0; p < TARGET_PALLETS; p++) {
            int start = p * CARTONS_PER_PALLET;
            int end = start + CARTONS_PER_PALLET;
            if (end > cartonCodes.size()) {
                break;
            }
            String palletCode = "PALLET-EXT-%03d-%s".formatted(p + 1, shortUuid());
            LocalDateTime bindTime = baseTime.plusDays(18L).plusHours(p + 1L)
                    .withSecond(44).withNano(0).truncatedTo(ChronoUnit.SECONDS);
            for (int i = start; i < end; i++) {
                String cartonCode = cartonCodes.get(i);
                rows.add(buildRow(palletCode, cartonCode, "PALLET", createdBy, bindTime));
                List<String> cartonChildren = cartonToChildren.get(cartonCode);
                if (cartonChildren != null) {
                    for (String traceCode : cartonChildren) {
                        appendAggregationLog(traceCode, ActionType.PALLETIZE, palletCode,
                                bindTime, createdBy, safeChains);
                    }
                }
            }
        }
        return rows;
    }

    /**
     * Append a PACK / PALLETIZE lifecycle log to the trace's existing chain
     * and bump the snapshot's tail hash + event time. No-op when the trace
     * has no chain (unit tests passing an empty map).
     */
    private void appendAggregationLog(
            String traceCode,
            ActionType actionType,
            String containerCode,
            LocalDateTime eventTime,
            DemoUserRef operator,
            Map<String, DemoChainBuilder.ChainResult> chainsByTrace
    ) {
        DemoChainBuilder.ChainResult chain = chainsByTrace.get(traceCode);
        if (chain == null) {
            return;
        }
        TraceSnapshot snapshot = chain.snapshot();
        String prevHash = HashUtil.safePrev(snapshot.getLastHash());
        LocalDateTime truncated = eventTime.truncatedTo(ChronoUnit.SECONDS);
        String remark = (actionType == ActionType.PALLETIZE ? "上托盘 → " : "装箱 → ") + containerCode;
        String operatorName = operator == null ? "system" : operator.username();
        TraceLifecycleLog log = logFactory.createLog(
                traceCode,
                snapshot.getSpuId(),
                actionType,
                snapshot.getCurrentNode(),
                containerCode,
                snapshot.getProvince(),
                snapshot.getCity(),
                remark,
                truncated,
                truncated,
                prevHash,
                null,
                operatorName
        );
        chain.logs().add(log);
        snapshot.setLastHash(log.getCurrentHash());
        snapshot.setLastEventTime(truncated);
        // lastLogId is back-filled by TraceBatchCommitter after PK assignment.
    }

    private static TraceAggregation buildRow(String parentCode, String childCode, String relationType,
                                             DemoUserRef createdBy, LocalDateTime bindTime) {
        TraceAggregation row = new TraceAggregation();
        row.setParentCode(parentCode);
        row.setChildCode(childCode);
        row.setRelationType(relationType);
        row.setActive(true);
        if (createdBy != null) {
            row.setCreateBy(createdBy.id());
            row.setCreateByUsername(createdBy.username());
        }
        row.setBindTime(bindTime);
        row.setReleaseTime(null);
        row.setRemark(DEMO_REMARK);
        return row;
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
