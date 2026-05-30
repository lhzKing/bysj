package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.config.TraceBatchProperties;
import com.example.trace.config.TraceDemoDataProperties;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.SysUser;
import com.example.trace.entity.TraceAssignBatch;
import com.example.trace.entity.TraceFlowTask;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceNode;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceAssignBatchMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceFlowTaskMapper;
import com.example.trace.mapper.TraceFlowTaskScanMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.mapper.TraceScanIdempotencyMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.impl.support.DemoAggregationFactory;
import com.example.trace.service.impl.support.DemoChainBuilder;
import com.example.trace.service.impl.support.DemoFlowTaskFactory;
import com.example.trace.service.impl.support.TraceBatchCommitter;
import com.example.trace.service.impl.support.TraceLogFactory;
import com.example.trace.util.SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceDemoDataServiceImplTest {

    @Mock private BasePartSpecMapper partSpecMapper;
    @Mock private TraceLifecycleLogMapper logMapper;
    @Mock private TraceSnapshotMapper snapshotMapper;
    @Mock private TraceCodeMapper traceCodeMapper;
    @Mock private TraceAssignBatchMapper assignBatchMapper;
    @Mock private TraceFlowTaskMapper flowTaskMapper;
    @Mock private TraceFlowTaskScanMapper flowTaskScanMapper;
    @Mock private TraceAggregationMapper aggregationMapper;
    @Mock private TraceScanIdempotencyMapper scanIdempotencyMapper;
    @Mock private TraceNodeMapper traceNodeMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private SignatureUtil signatureUtil;
    @Mock private TraceDemoDataProperties traceDemoDataProperties;

    private TraceBatchCommitter batchCommitter;
    private TraceDemoDataServiceImpl service;

    @BeforeEach
    void setUp() {
        TraceBatchProperties batchProperties = new TraceBatchProperties();
        // Two-pass construction so the @Lazy self-proxy points at the real instance.
        batchCommitter = new TraceBatchCommitter(logMapper, snapshotMapper, traceCodeMapper,
                assignBatchMapper, flowTaskMapper, flowTaskScanMapper, aggregationMapper,
                batchProperties, null);
        batchCommitter = new TraceBatchCommitter(logMapper, snapshotMapper, traceCodeMapper,
                assignBatchMapper, flowTaskMapper, flowTaskScanMapper, aggregationMapper,
                batchProperties, batchCommitter);
        TraceLogFactory logFactory = new TraceLogFactory(signatureUtil);
        DemoChainBuilder chainBuilder = new DemoChainBuilder(logFactory);
        DemoFlowTaskFactory flowTaskFactory = new DemoFlowTaskFactory();
        DemoAggregationFactory aggregationFactory = new DemoAggregationFactory(logFactory);
        service = new TraceDemoDataServiceImpl(
                partSpecMapper, logMapper, snapshotMapper, traceCodeMapper,
                assignBatchMapper, flowTaskMapper, flowTaskScanMapper, aggregationMapper,
                scanIdempotencyMapper, traceNodeMapper, sysUserMapper,
                traceDemoDataProperties, batchCommitter,
                chainBuilder, flowTaskFactory, aggregationFactory);
    }

    @Test
    void clearTraceData_deletesAllEightFlowTablesInFkOrderAndReturnsCounts() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);
        when(flowTaskScanMapper.selectCount(null)).thenReturn(700L);
        when(flowTaskMapper.selectCount(null)).thenReturn(60L);
        when(aggregationMapper.selectCount(null)).thenReturn(330L);
        when(scanIdempotencyMapper.selectCount(null)).thenReturn(1L);
        when(logMapper.selectCount(null)).thenReturn(3500L);
        when(snapshotMapper.selectCount(null)).thenReturn(600L);
        when(traceCodeMapper.selectCount(null)).thenReturn(600L);
        when(assignBatchMapper.selectCount(null)).thenReturn(30L);

        Map<String, Object> result = service.clearTraceData("superadmin", "SUPER_ADMIN");

        // FK delete order: children before parents.
        InOrder inOrder = inOrder(flowTaskScanMapper, flowTaskMapper, aggregationMapper,
                scanIdempotencyMapper, logMapper, snapshotMapper, traceCodeMapper, assignBatchMapper);
        inOrder.verify(flowTaskScanMapper).delete(null);
        inOrder.verify(flowTaskMapper).delete(null);
        inOrder.verify(aggregationMapper).delete(null);
        inOrder.verify(scanIdempotencyMapper).delete(null);
        inOrder.verify(logMapper).delete(null);
        inOrder.verify(snapshotMapper).delete(null);
        inOrder.verify(traceCodeMapper).delete(null);
        inOrder.verify(assignBatchMapper).delete(null);

        assertThat(result)
                .containsEntry("deletedFlowTaskScans", 700L)
                .containsEntry("deletedFlowTasks", 60L)
                .containsEntry("deletedAggregations", 330L)
                .containsEntry("deletedIdempotencyKeys", 1L)
                .containsEntry("deletedLogs", 3500L)
                .containsEntry("deletedSnapshots", 600L)
                .containsEntry("deletedTraceCodes", 600L)
                .containsEntry("deletedBatches", 30L);
    }

    @Test
    void generateSampleData_returnsLifecycleSummaryAndPersistsCompliantChains() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);
        when(traceDemoDataProperties.getMaxGenerateCount()).thenReturn(500);
        when(traceNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                node(1L, "FAC-A", "FACTORY", "北京", "北京市"),
                node(2L, "WH-A", "WAREHOUSE", "江苏", "苏州市"),
                node(3L, "WH-B", "WAREHOUSE", "广东", "广州市"),
                node(4L, "LG-A", "LOGISTICS", "上海", "上海市"),
                node(5L, "CUST-A", "CUSTOMER", "湖北", "武汉市")
        ));
        when(partSpecMapper.selectList(any(Wrapper.class))).thenReturn(List.of(part(100L, "SPU-001")));
        when(sysUserMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(user(101L, "producer-demo")))
                .thenReturn(List.of(user(102L, "warehouse-demo")))
                .thenReturn(List.of(user(103L, "logistics-demo")));
        when(signatureUtil.sign(anyString())).thenReturn("MOCK_SIGNATURE");
        when(signatureUtil.getKeyId()).thenReturn("test-key");
        when(signatureUtil.getKeyVersion()).thenReturn(1);

        AtomicLong idSeq = new AtomicLong(1L);
        when(assignBatchMapper.insert(any(TraceAssignBatch.class))).thenAnswer(invocation -> {
            TraceAssignBatch batch = invocation.getArgument(0);
            batch.setId(idSeq.getAndIncrement());
            return 1;
        });
        when(logMapper.insert(any(TraceLifecycleLog.class))).thenAnswer(invocation -> {
            TraceLifecycleLog log = invocation.getArgument(0);
            log.setId(idSeq.getAndIncrement());
            return 1;
        });
        when(flowTaskMapper.insert(any(TraceFlowTask.class))).thenAnswer(invocation -> {
            TraceFlowTask task = invocation.getArgument(0);
            task.setId(idSeq.getAndIncrement());
            return 1;
        });

        Map<String, Object> result = service.generateSampleData(30, "superadmin", "SUPER_ADMIN");

        assertThat(result)
                .containsEntry("traceCodes", 30)
                .containsEntry("snapshots", 30)
                .containsEntry("lifecycleValidation", "OK")
                .containsKeys("lifecycleModel", "coreLifecyclePrefix", "actionCounts",
                        "snapshotStatusCounts", "codeStatusCounts", "terminalSummary", "sampleLifecyclePaths");

        @SuppressWarnings("unchecked")
        Map<String, Integer> actionCounts = (Map<String, Integer>) result.get("actionCounts");
        assertThat(actionCounts)
                .containsEntry("INIT", 30)
                .containsEntry("PRINT_CODE", 30)
                .containsEntry("ACTIVATE_CODE", 30)
                .containsEntry("INBOUND", 30);

        @SuppressWarnings("unchecked")
        Map<String, Object> terminalSummary = (Map<String, Object>) result.get("terminalSummary");
        assertThat(terminalSummary)
                .containsEntry("totalChains", 30)
                .containsEntry("finishedGoodsInboundBeforeOutboundChains", 30)
                .containsEntry("transferredTerminalBlockedFromFurtherInbound", true)
                .containsEntry("transferMeansTransitOnly", true)
                .containsEntry("deliverMeansFinalTransferred", true);

        @SuppressWarnings("unchecked")
        List<String> paths = (List<String>) result.get("sampleLifecyclePaths");
        assertThat(paths).isNotEmpty();
        assertThat(paths).allSatisfy(path -> {
            assertThat(path).startsWith("INIT -> PRINT_CODE -> ACTIVATE_CODE -> INBOUND");
            assertThat(path).doesNotContain("ACTIVATE_CODE -> OUTBOUND");
        });

        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(logMapper, atLeastOnce()).insert(logCaptor.capture());
        Map<String, List<String>> actionsByTrace = logCaptor.getAllValues().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        TraceLifecycleLog::getTraceCode,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(TraceLifecycleLog::getActionType, java.util.stream.Collectors.toList())
                ));
        assertThat(actionsByTrace).hasSize(30);
        actionsByTrace.forEach((traceCode, actions) -> assertLifecycleCompliant(traceCode, actions));
    }

    @Test
    void generateSampleData_rejectsWhenTraceNodeOrPartSpecEmpty() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);
        when(traceDemoDataProperties.getMaxGenerateCount()).thenReturn(500);
        when(traceNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(partSpecMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateSampleData(10, "superadmin", "SUPER_ADMIN"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("seed-master-data");
    }

    @Test
    void generateSampleData_rejectsWhenAnyNodeTypeMissing() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);
        when(traceDemoDataProperties.getMaxGenerateCount()).thenReturn(500);
        // Only FACTORY+WAREHOUSE present — LOGISTICS and CUSTOMER missing.
        when(traceNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                node(1L, "FAC-A", "FACTORY", "北京", "北京市"),
                node(2L, "WH-A", "WAREHOUSE", "江苏", "苏州市")
        ));
        when(partSpecMapper.selectList(any(Wrapper.class))).thenReturn(List.of(part(1L, "SPU-001")));

        assertThatThrownBy(() -> service.generateSampleData(10, "superadmin", "SUPER_ADMIN"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("每种节点类型");
    }

    @Test
    void generateSampleData_rejectsCountAboveMaximum() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(true);
        when(traceDemoDataProperties.getMaxGenerateCount()).thenReturn(5);

        assertThatThrownBy(() -> service.generateSampleData(6, "admin", "ADMIN"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.PARAM_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("count 必须在 1 到 5 之间");
                });

        verifyNoInteractions(partSpecMapper, logMapper, snapshotMapper, traceNodeMapper);
        verify(signatureUtil, org.mockito.Mockito.never()).sign(anyString());
    }

    @Test
    void clearTraceData_rejectsWhenEndpointDisabled() {
        when(traceDemoDataProperties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.clearTraceData("admin", "ADMIN"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(BizCode.FORBIDDEN);
                    assertThat(exception.getMessage()).isEqualTo("当前环境已禁用示例数据管理接口");
                });

        verifyNoInteractions(logMapper, snapshotMapper, flowTaskMapper, aggregationMapper);
    }

    private static void assertLifecycleCompliant(String traceCode, List<String> actions) {
        assertThat(actions)
                .as(traceCode + " core lifecycle")
                .startsWith("INIT", "PRINT_CODE", "ACTIVATE_CODE", "INBOUND");
        assertThat(actions).as(traceCode).doesNotContainSequence("ACTIVATE_CODE", "OUTBOUND");

        int inbound = actions.indexOf("INBOUND");
        int outbound = actions.indexOf("OUTBOUND");
        int transfer = actions.indexOf("TRANSFER");
        int deliver = actions.indexOf("DELIVER");
        int exceptionOpen = actions.indexOf("EXCEPTION_OPEN");

        assertThat(inbound).as(traceCode + " first inbound index").isEqualTo(3);
        if (outbound >= 0) {
            assertThat(outbound).as(traceCode + " outbound after inbound").isGreaterThan(inbound);
        }
        if (transfer >= 0) {
            assertThat(outbound).as(traceCode + " transfer requires outbound").isGreaterThanOrEqualTo(0);
            assertThat(transfer).as(traceCode + " transfer after outbound").isGreaterThan(outbound);
        }
        if (deliver >= 0) {
            assertThat(outbound).as(traceCode + " deliver requires outbound").isGreaterThanOrEqualTo(0);
            assertThat(deliver).as(traceCode + " deliver after outbound").isGreaterThan(outbound);
            assertThat(deliver).as(traceCode + " deliver is terminal tail").isEqualTo(actions.size() - 1);
        }
        if (transfer >= 0 && deliver >= 0) {
            assertThat(transfer).as(traceCode + " transfer before deliver").isLessThan(deliver);
        }
        if (exceptionOpen >= 0) {
            assertThat(exceptionOpen).as(traceCode + " exception is tail").isEqualTo(actions.size() - 1);
        }
    }

    private static SysUser user(Long id, String username) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        u.setStatus(1);
        return u;
    }

    private static TraceNode node(Long id, String code, String type, String province, String city) {
        TraceNode n = new TraceNode();
        n.setId(id);
        n.setNodeCode(code);
        n.setNodeName(code);
        n.setNodeType(type);
        n.setProvince(province);
        n.setCity(city);
        n.setEnabled(true);
        return n;
    }

    private static BasePartSpec part(Long id, String partCode) {
        BasePartSpec p = new BasePartSpec();
        p.setId(id);
        p.setPartCode(partCode);
        p.setPartName(partCode);
        p.setPartType("阀门类");
        p.setEnabled(true);
        return p;
    }
}
