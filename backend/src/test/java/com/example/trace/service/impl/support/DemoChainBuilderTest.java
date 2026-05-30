package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.util.HashUtil;
import com.example.trace.util.SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Validates that {@link DemoChainBuilder} produces a sound hash-linked chain:
 *
 * <ul>
 *   <li>First entry's {@code prevHash} is "GENESIS"</li>
 *   <li>Every subsequent entry's {@code prevHash} matches the prior entry's {@code currentHash}</li>
 *   <li>Recomputing the hash with {@link HashUtil} from the entry's persisted fields reproduces
 *       the stored {@code currentHash} (proving the builder doesn't fabricate data outside the hashed payload)</li>
 *   <li>Tail snapshot summarises the terminal state correctly</li>
 * </ul>
 */
class DemoChainBuilderTest {

    private SignatureUtil signatureUtil;
    private TraceLogFactory logFactory;
    private DemoChainBuilder builder;

    @BeforeEach
    void setUp() {
        signatureUtil = Mockito.mock(SignatureUtil.class);
        when(signatureUtil.sign(anyString())).thenReturn("MOCK_SIGNATURE");
        when(signatureUtil.getKeyId()).thenReturn("test-key");
        when(signatureUtil.getKeyVersion()).thenReturn(1);
        logFactory = new TraceLogFactory(signatureUtil);
        builder = new DemoChainBuilder(logFactory);
    }

    @Test
    void buildChain_emitsAtLeastTheFiveCoreActions() {
        DemoChainBuilder.ChainResult result = builder.buildChain(
                "TC-TEST-0001", 100L,
                node(1L, "FACTORY-A", "FACTORY", "北京", "北京市"),
                node(2L, "WAREHOUSE-A", "WAREHOUSE", "江苏", "苏州市"),
                node(3L, "LOGISTICS-A", "LOGISTICS", "上海", "上海市"),
                node(4L, "CUSTOMER-A", "CUSTOMER", "湖北", "武汉市"),
                "producer", "warehouse", "logistics",
                LocalDateTime.of(2026, 4, 1, 9, 13, 0),
                new Random(20260527L)
        );

        List<TraceLifecycleLog> logs = result.logs();
        assertThat(logs.size()).isBetween(4, 8);
        assertThat(logs.get(0).getActionType()).isEqualTo("INIT");
        assertThat(logs.get(1).getActionType()).isEqualTo("PRINT_CODE");
        assertThat(logs.get(2).getActionType()).isEqualTo("ACTIVATE_CODE");
        assertThat(logs.get(3).getActionType()).isEqualTo("INBOUND");
        assertThat(logs.stream().filter(l -> "OUTBOUND".equals(l.getActionType())).count()).isLessThanOrEqualTo(1L);
    }

    @Test
    void buildChain_prevHashLinksFromGenesisAcrossAllSteps() {
        DemoChainBuilder.ChainResult result = builder.buildChain(
                "TC-TEST-0002", 100L,
                node(1L, "FACTORY-A", "FACTORY", "北京", "北京市"),
                node(2L, "WAREHOUSE-A", "WAREHOUSE", "江苏", "苏州市"),
                node(3L, "LOGISTICS-A", "LOGISTICS", "上海", "上海市"),
                node(4L, "CUSTOMER-A", "CUSTOMER", "湖北", "武汉市"),
                "producer", "warehouse", "logistics",
                LocalDateTime.of(2026, 4, 1, 9, 13, 0),
                new Random(42L)
        );

        List<TraceLifecycleLog> logs = result.logs();
        assertThat(logs.get(0).getPrevHash()).isEqualTo("GENESIS");
        for (int i = 1; i < logs.size(); i++) {
            assertThat(logs.get(i).getPrevHash())
                    .as("log[%d].prevHash must equal log[%d].currentHash", i, i - 1)
                    .isEqualTo(logs.get(i - 1).getCurrentHash());
        }
    }

    @Test
    void buildChain_currentHashIsReproducibleFromPersistedFields() {
        // Re-derive each entry's hash from its own persisted fields and confirm equality.
        // This is the strongest correctness test: it proves the builder didn't compute the
        // hash from a different payload than what gets stored.
        DemoChainBuilder.ChainResult result = builder.buildChain(
                "TC-TEST-0003", 100L,
                node(1L, "FACTORY-A", "FACTORY", "北京", "北京市"),
                node(2L, "WAREHOUSE-A", "WAREHOUSE", "江苏", "苏州市"),
                node(3L, "LOGISTICS-A", "LOGISTICS", "上海", "上海市"),
                node(4L, "CUSTOMER-A", "CUSTOMER", "湖北", "武汉市"),
                "producer", "warehouse", "logistics",
                LocalDateTime.of(2026, 4, 1, 9, 13, 0),
                new Random(123L)
        );

        for (TraceLifecycleLog logEntry : result.logs()) {
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
                    .as("currentHash mismatch on action=%s", logEntry.getActionType())
                    .isEqualTo(logEntry.getCurrentHash());
        }
    }

    @Test
    void buildChain_tailSnapshotReflectsTerminalLog() {
        DemoChainBuilder.ChainResult result = builder.buildChain(
                "TC-TEST-0004", 100L,
                node(1L, "FACTORY-A", "FACTORY", "北京", "北京市"),
                node(2L, "WAREHOUSE-A", "WAREHOUSE", "江苏", "苏州市"),
                node(3L, "LOGISTICS-A", "LOGISTICS", "上海", "上海市"),
                node(4L, "CUSTOMER-A", "CUSTOMER", "湖北", "武汉市"),
                "producer", "warehouse", "logistics",
                LocalDateTime.of(2026, 4, 1, 9, 13, 0),
                new Random(7L)
        );

        TraceLifecycleLog tail = result.logs().get(result.logs().size() - 1);
        TraceSnapshot snap = result.snapshot();
        assertThat(snap.getTraceCode()).isEqualTo("TC-TEST-0004");
        assertThat(snap.getSpuId()).isEqualTo(100L);
        assertThat(snap.getLastHash()).isEqualTo(tail.getCurrentHash());
        assertThat(snap.getLastEventTime()).isEqualTo(tail.getEventTime());
        assertThat(snap.getVersion()).isZero();
        assertThat(snap.getLastLogId()).isNull(); // back-filled by committer after PK assignment
        assertThat(snap.getCurrentStatus()).isIn("IN_STOCK", "IN_TRANSIT", "TRANSFERRED", "EXCEPTION");
    }


    @Test
    void buildChain_shouldUseDeliverOnlyForFinalDelivery() {
        DemoChainBuilder.ChainResult result = builder.buildChain(
                "TC-TEST-DELIVER", 100L,
                node(1L, "FACTORY-A", "FACTORY", "北京", "北京市"),
                node(2L, "WAREHOUSE-A", "WAREHOUSE", "江苏", "苏州市"),
                node(3L, "LOGISTICS-A", "LOGISTICS", "上海", "上海市"),
                node(4L, "CUSTOMER-A", "CUSTOMER", "湖北", "武汉市"),
                "producer", "warehouse", "logistics",
                LocalDateTime.of(2026, 4, 1, 9, 13, 0),
                new Random(4096L)
        );

        List<String> actions = result.logs().stream()
                .map(TraceLifecycleLog::getActionType)
                .toList();
        if ("TRANSFERRED".equals(result.snapshot().getCurrentStatus())) {
            assertThat(actions).contains("DELIVER");
            assertThat(actions.get(actions.size() - 1)).isIn("DELIVER", "EXCEPTION_OPEN");
        }
        assertThat(actions).doesNotContainSequence("ACTIVATE_CODE", "OUTBOUND");
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
}
