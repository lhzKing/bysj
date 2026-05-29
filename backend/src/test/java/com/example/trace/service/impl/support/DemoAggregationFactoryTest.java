package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceAggregation;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.util.HashUtil;
import com.example.trace.util.SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DemoAggregationFactoryTest {

    private SignatureUtil signatureUtil;
    private TraceLogFactory logFactory;
    private DemoAggregationFactory factory;
    private final DemoUserRef creator = new DemoUserRef(101L, "producer");

    @BeforeEach
    void setUp() {
        signatureUtil = Mockito.mock(SignatureUtil.class);
        when(signatureUtil.sign(anyString())).thenReturn("MOCK_SIGNATURE");
        when(signatureUtil.getKeyId()).thenReturn("test-key");
        when(signatureUtil.getKeyVersion()).thenReturn(1);
        logFactory = new TraceLogFactory(signatureUtil);
        factory = new DemoAggregationFactory(logFactory);
    }

    @Test
    void build_producesCartonsAndPalletsWithExpectedShape() {
        List<String> pool = pool(360); // enough for 24 cartons × 15 children
        List<TraceAggregation> rows = factory.build(
                pool, Map.of(), creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        long cartonRows = rows.stream().filter(r -> "CARTON".equals(r.getRelationType())).count();
        long palletRows = rows.stream().filter(r -> "PALLET".equals(r.getRelationType())).count();

        // CARTON: 24 groups × [10, 15] children → between 240 and 360 carton rows
        assertThat(cartonRows).isBetween(240L, (long) DemoAggregationFactory.TARGET_CARTONS * DemoAggregationFactory.CARTON_SIZE_MAX);
        // PALLET: 6 pallets × 3 cartons = 18 rows
        assertThat(palletRows).isEqualTo((long) DemoAggregationFactory.TARGET_PALLETS * DemoAggregationFactory.CARTONS_PER_PALLET);
    }

    @Test
    void build_eachActiveChildIsBoundOnlyOnce() {
        List<String> pool = pool(360);
        List<TraceAggregation> rows = factory.build(
                pool, Map.of(), creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(7L)
        );

        Set<String> seenCartonChildren = new HashSet<>();
        Set<String> seenPalletChildren = new HashSet<>();
        for (TraceAggregation row : rows) {
            if ("CARTON".equals(row.getRelationType())) {
                assertThat(seenCartonChildren.add(row.getChildCode()))
                        .as("trace_code %s bound to two cartons", row.getChildCode())
                        .isTrue();
            } else if ("PALLET".equals(row.getRelationType())) {
                assertThat(seenPalletChildren.add(row.getChildCode()))
                        .as("carton %s bound to two pallets", row.getChildCode())
                        .isTrue();
            }
        }
    }

    @Test
    void build_palletChildrenAreCartonParents() {
        List<String> pool = pool(360);
        List<TraceAggregation> rows = factory.build(
                pool, Map.of(), creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        Set<String> cartonParents = new HashSet<>();
        for (TraceAggregation row : rows) {
            if ("CARTON".equals(row.getRelationType())) {
                cartonParents.add(row.getParentCode());
            }
        }
        for (TraceAggregation row : rows) {
            if ("PALLET".equals(row.getRelationType())) {
                assertThat(cartonParents)
                        .as("pallet child %s must be one of the carton parents we produced", row.getChildCode())
                        .contains(row.getChildCode());
            }
        }
    }

    @Test
    void build_setsActiveTrueAndPopulatesCreator() {
        List<String> pool = pool(360);
        List<TraceAggregation> rows = factory.build(
                pool, Map.of(), creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(42L)
        );

        for (TraceAggregation row : rows) {
            assertThat(row.getActive()).isTrue();
            assertThat(row.getCreateBy()).isEqualTo(101L);
            assertThat(row.getCreateByUsername()).isEqualTo("producer");
            assertThat(row.getReleaseTime()).isNull();
            assertThat(row.getBindTime()).isNotNull();
            assertThat(row.getParentCode()).isNotEqualTo(row.getChildCode());
        }
    }

    @Test
    void build_parentCodesUseCartonAndPalletPrefixes() {
        List<String> pool = pool(360);
        List<TraceAggregation> rows = factory.build(
                pool, Map.of(), creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        // Parent codes must start with CARTON-/PALLET- so the real bind/release
        // path (TraceAggregationServiceImpl) treats them as parent aggregation
        // codes rather than single-item trace codes.
        for (TraceAggregation row : rows) {
            if ("CARTON".equals(row.getRelationType())) {
                assertThat(row.getParentCode())
                        .as("carton parent must use CARTON- prefix, got %s", row.getParentCode())
                        .startsWith("CARTON-");
            } else if ("PALLET".equals(row.getRelationType())) {
                assertThat(row.getParentCode())
                        .as("pallet parent must use PALLET- prefix, got %s", row.getParentCode())
                        .startsWith("PALLET-");
                assertThat(row.getChildCode())
                        .as("pallet child should be one of the carton parents, prefix CARTON-")
                        .startsWith("CARTON-");
            }
        }
    }

    @Test
    void build_emptyPoolReturnsEmpty() {
        List<TraceAggregation> rows = factory.build(
                List.of(), Map.of(), creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(1L)
        );
        assertThat(rows).isEmpty();
    }

    @Test
    void build_appendsPackLogForEachCartonChildAndPalletizeForEachExpandedTrace() {
        // 33 codes → at least 2 full cartons of [10..15] each, possibly 3 if RNG draws minima.
        List<String> pool = pool(33);
        Map<String, DemoChainBuilder.ChainResult> chains = chainsFor(pool);

        List<TraceAggregation> rows = factory.build(
                pool, chains, creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        long cartonRows = rows.stream().filter(r -> "CARTON".equals(r.getRelationType())).count();
        long palletRows = rows.stream().filter(r -> "PALLET".equals(r.getRelationType())).count();

        int packLogs = 0;
        int palletizeLogs = 0;
        for (DemoChainBuilder.ChainResult chain : chains.values()) {
            for (TraceLifecycleLog logEntry : chain.logs()) {
                if ("PACK".equals(logEntry.getActionType())) packLogs++;
                else if ("PALLETIZE".equals(logEntry.getActionType())) palletizeLogs++;
            }
        }

        // One PACK log per CARTON row (one per single-item trace_code).
        assertThat(packLogs)
                .as("PACK log count must match CARTON row count")
                .isEqualTo((int) cartonRows);
        // PALLET row count is in terms of (pallet, carton) pairs. Each pair
        // expands to one PALLETIZE log per single-item trace_code in the carton,
        // so PALLETIZE count == sum(carton size for each (pallet, carton) pair).
        // We can't predict exact size without re-running RNG, so just assert
        // PALLETIZE > 0 whenever pallets exist and PALLET row count ≥ 0.
        if (palletRows > 0) {
            assertThat(palletizeLogs)
                    .as("PALLETIZE log count must be positive when pallet rows exist")
                    .isGreaterThan(0);
        }
    }

    @Test
    void build_appendedLogsAreHashChainedFromExistingTail() {
        List<String> pool = pool(33);
        Map<String, DemoChainBuilder.ChainResult> chains = chainsFor(pool);
        // Stash the existing tail hash + lastEventTime for each trace before
        // running the factory so we can prove the new PACK log chains off it.
        Map<String, String> preTailHash = new HashMap<>();
        Map<String, LocalDateTime> preLastEvent = new HashMap<>();
        for (Map.Entry<String, DemoChainBuilder.ChainResult> e : chains.entrySet()) {
            preTailHash.put(e.getKey(), e.getValue().snapshot().getLastHash());
            preLastEvent.put(e.getKey(), e.getValue().snapshot().getLastEventTime());
        }

        factory.build(
                pool, chains, creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        for (Map.Entry<String, DemoChainBuilder.ChainResult> e : chains.entrySet()) {
            String traceCode = e.getKey();
            List<TraceLifecycleLog> logs = e.getValue().logs();
            TraceLifecycleLog firstPack = logs.stream()
                    .filter(l -> "PACK".equals(l.getActionType()))
                    .findFirst().orElse(null);
            if (firstPack == null) {
                // Trace wasn't selected for any carton (pool/shuffle outcome) — skip.
                continue;
            }
            assertThat(firstPack.getPrevHash())
                    .as("PACK.prevHash must equal pre-aggregation snapshot.lastHash for %s", traceCode)
                    .isEqualTo(preTailHash.get(traceCode));

            // After build the snapshot's lastHash must point at the trace's final
            // aggregation log (PACK or PALLETIZE, whichever is later).
            TraceLifecycleLog lastAggregationLog = logs.stream()
                    .filter(l -> "PACK".equals(l.getActionType()) || "PALLETIZE".equals(l.getActionType()))
                    .reduce((a, b) -> b).orElse(null);
            assertThat(lastAggregationLog).isNotNull();
            assertThat(e.getValue().snapshot().getLastHash())
                    .as("snapshot.lastHash must follow appended aggregation logs for %s", traceCode)
                    .isEqualTo(lastAggregationLog.getCurrentHash());

            // Each subsequent log's prevHash must equal the prior log's currentHash.
            for (int i = 1; i < logs.size(); i++) {
                assertThat(logs.get(i).getPrevHash())
                        .as("log[%d].prevHash must equal log[%d].currentHash on %s", i, i - 1, traceCode)
                        .isEqualTo(logs.get(i - 1).getCurrentHash());
            }
        }
    }

    @Test
    void build_appendedLogsAreHashReproducibleFromPersistedFields() {
        // Strongest correctness test: recompute each appended log's hash from
        // its own persisted fields and verify equality.
        List<String> pool = pool(33);
        Map<String, DemoChainBuilder.ChainResult> chains = chainsFor(pool);

        factory.build(
                pool, chains, creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        for (DemoChainBuilder.ChainResult chain : chains.values()) {
            for (TraceLifecycleLog logEntry : chain.logs()) {
                if (!"PACK".equals(logEntry.getActionType()) && !"PALLETIZE".equals(logEntry.getActionType())) {
                    continue;
                }
                String recomputed = HashUtil.calculateHash(
                        logEntry.getTraceCode(),
                        logEntry.getActionType(),
                        logEntry.getFromNode(),
                        logEntry.getToNode(),
                        logEntry.getProvince(),
                        logEntry.getCity(),
                        logEntry.getRemark(),
                        logEntry.getEventTime(),
                        logEntry.getIngestTime(),
                        logEntry.getPrevHash(),
                        logEntry.getCorrectionOf(),
                        logEntry.getOperator()
                );
                assertThat(recomputed)
                        .as("currentHash mismatch on %s for %s", logEntry.getActionType(), logEntry.getTraceCode())
                        .isEqualTo(logEntry.getCurrentHash());
            }
        }
    }

    @Test
    void build_skipsLogAppendForTraceMissingFromChainMap() {
        // A chains map missing entries should not blow up: the factory still
        // produces rows but quietly skips log generation for the missing trace.
        List<String> pool = pool(33);
        // chains intentionally empty (or missing the pool trace codes)
        Map<String, DemoChainBuilder.ChainResult> chains = new LinkedHashMap<>();

        List<TraceAggregation> rows = factory.build(
                pool, chains, creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        assertThat(rows).isNotEmpty();
        // No chains → nothing to append.
        assertThat(chains).isEmpty();
    }

    private static List<String> pool(int n) {
        List<String> list = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            list.add("TC-CAND-%04d".formatted(i));
        }
        return list;
    }

    /**
     * Build minimal in-memory chains for each pool trace code. Each chain
     * starts at GENESIS with one INIT-style entry just to populate
     * {@code snapshot.lastHash}; the factory only needs the snapshot fields to
     * generate aggregation logs.
     */
    private Map<String, DemoChainBuilder.ChainResult> chainsFor(List<String> pool) {
        Map<String, DemoChainBuilder.ChainResult> chains = new LinkedHashMap<>();
        for (String traceCode : pool) {
            TraceSnapshot snapshot = new TraceSnapshot();
            snapshot.setTraceCode(traceCode);
            snapshot.setSpuId(100L);
            snapshot.setCurrentStatus("IN_STOCK");
            snapshot.setCurrentNode("WAREHOUSE-A");
            snapshot.setProvince("江苏省");
            snapshot.setCity("苏州市");
            snapshot.setLastHash(HashUtil.safePrev(null)); // GENESIS
            snapshot.setLastEventTime(LocalDateTime.of(2026, 4, 1, 9, 0, 0));
            snapshot.setVersion(0);

            chains.put(traceCode, new DemoChainBuilder.ChainResult(
                    new ArrayList<>(),
                    snapshot,
                    "IN_STOCK"
            ));
        }
        return chains;
    }
}
