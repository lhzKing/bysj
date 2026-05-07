package com.example.trace.service.impl;

import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceAggregationBindRequest;
import com.example.trace.dto.TraceAggregationReleaseRequest;
import com.example.trace.dto.TraceAggregationResponse;
import com.example.trace.entity.TraceAggregation;
import com.example.trace.entity.TraceCode;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceAggregationRelationType;
import com.example.trace.mapper.TraceAggregationMapper;
import com.example.trace.mapper.TraceCodeMapper;
import com.example.trace.mapper.TraceLifecycleLogMapper;
import com.example.trace.mapper.TraceSnapshotMapper;
import com.example.trace.service.impl.support.TraceLogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceAggregationServiceImplTest {

    @Mock
    private TraceAggregationMapper traceAggregationMapper;
    @Mock
    private TraceCodeMapper traceCodeMapper;
    @Mock
    private TraceSnapshotMapper traceSnapshotMapper;
    @Mock
    private TraceLifecycleLogMapper traceLifecycleLogMapper;
    @Mock
    private TraceLogFactory traceLogFactory;

    private TraceAggregationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraceAggregationServiceImpl(
                traceAggregationMapper,
                traceCodeMapper,
                traceSnapshotMapper,
                traceLifecycleLogMapper,
                traceLogFactory
        );
        Mockito.lenient().when(traceSnapshotMapper.updateById(any(TraceSnapshot.class))).thenReturn(1);
    }

    @Test
    void bindChild_shouldPersistCartonToSingleTraceCodeRelation() {
        TraceAggregationBindRequest request = bindRequest(" carton-001 ", " trace-001 ", TraceAggregationRelationType.CARTON);
        when(traceAggregationMapper.selectActiveRelation("CARTON-001", "TRACE-001")).thenReturn(null);
        when(traceAggregationMapper.selectActiveParentsByChild("TRACE-001")).thenReturn(List.of());
        when(traceCodeMapper.selectByTraceCode("TRACE-001")).thenReturn(traceCode("TRACE-001"));
        when(traceSnapshotMapper.selectById("TRACE-001")).thenReturn(snapshot("TRACE-001", "IN_STOCK"));
        when(traceLogFactory.createLog(
                eq("TRACE-001"), eq(1L), eq(ActionType.PACK), eq("北京仓"), eq("CARTON-001"),
                eq("北京"), eq("北京市"), any(), any(), any(), eq("hash-0"), any(), eq("operator-a")
        )).thenAnswer(invocation -> lifecycleLog(
                501L,
                "pack-hash",
                ((ActionType) invocation.getArgument(2)).getCode(),
                invocation.getArgument(4)
        ));
        when(traceAggregationMapper.insert(any(TraceAggregation.class))).thenAnswer(invocation -> {
            TraceAggregation relation = invocation.getArgument(0);
            relation.setId(99L);
            return 1;
        });

        TraceAggregationResponse response = service.bindChild(request, 7L, "operator-a");

        ArgumentCaptor<TraceAggregation> relationCaptor = ArgumentCaptor.forClass(TraceAggregation.class);
        verify(traceAggregationMapper).insert(relationCaptor.capture());
        TraceAggregation relation = relationCaptor.getValue();
        assertThat(relation.getParentCode()).isEqualTo("CARTON-001");
        assertThat(relation.getChildCode()).isEqualTo("TRACE-001");
        assertThat(relation.getRelationType()).isEqualTo(TraceAggregationRelationType.CARTON.getCode());
        assertThat(relation.getActive()).isTrue();
        assertThat(relation.getCreateBy()).isEqualTo(7L);
        assertThat(relation.getCreateByUsername()).isEqualTo("operator-a");
        assertThat(relation.getBindTime()).isNotNull();
        assertThat(relation.getRemark()).isEqualTo("装箱");
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getRelationType()).isEqualTo(TraceAggregationRelationType.CARTON);
        assertThat(response.getRelationTypeLabel()).isEqualTo("箱码");
        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(traceLifecycleLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.PACK.getCode());
        assertThat(logCaptor.getValue().getToNode()).isEqualTo("CARTON-001");
        verify(traceSnapshotMapper).updateById(any(TraceSnapshot.class));
    }

    @Test
    void bindChild_shouldAllowCartonToPalletRelation() {
        TraceAggregationBindRequest request = bindRequest("pallet-001", "carton-001", TraceAggregationRelationType.PALLET);
        when(traceAggregationMapper.selectActiveParentsByChild("CARTON-001")).thenReturn(List.of());
        when(traceAggregationMapper.selectActiveChildrenByParent("CARTON-001")).thenReturn(List.of());

        TraceAggregationResponse response = service.bindChild(request, 7L, "operator-a");

        assertThat(response.getParentCode()).isEqualTo("PALLET-001");
        assertThat(response.getChildCode()).isEqualTo("CARTON-001");
        assertThat(response.getRelationType()).isEqualTo(TraceAggregationRelationType.PALLET);
        verify(traceAggregationMapper).insert(any(TraceAggregation.class));
    }

    @Test
    void bindChild_shouldRejectExistingSingleTraceCodeAsParent() {
        TraceAggregationBindRequest request = bindRequest("SINGLE-ITEM-001", "TRACE-001", TraceAggregationRelationType.CARTON);
        when(traceCodeMapper.selectByTraceCode("SINGLE-ITEM-001")).thenReturn(traceCode("SINGLE-ITEM-001"));

        assertThatThrownBy(() -> service.bindChild(request, 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.PARAM_ERROR));
    }

    @Test
    void bindChild_shouldRejectDuplicateActiveRelation() {
        TraceAggregationBindRequest request = bindRequest("carton-001", "trace-001", TraceAggregationRelationType.CARTON);
        when(traceAggregationMapper.selectActiveRelation("CARTON-001", "TRACE-001"))
                .thenReturn(relation(1L, "CARTON-001", "TRACE-001", TraceAggregationRelationType.CARTON, true));

        assertThatThrownBy(() -> service.bindChild(request, 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.CONFLICT));
    }

    @Test
    void bindChild_shouldRejectCartonRelationWhenSingleCodeDoesNotExist() {
        TraceAggregationBindRequest request = bindRequest("carton-001", "trace-404", TraceAggregationRelationType.CARTON);
        when(traceAggregationMapper.selectActiveParentsByChild("TRACE-404")).thenReturn(List.of());
        when(traceCodeMapper.selectByTraceCode("TRACE-404")).thenReturn(null);

        assertThatThrownBy(() -> service.bindChild(request, 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.TRACE_NOT_FOUND));
    }

    @Test
    void bindChild_shouldRejectSingleCodeAlreadyInCartonWhenBindingDirectlyToPallet() {
        TraceAggregationBindRequest request = bindRequest("pallet-001", "trace-001", TraceAggregationRelationType.PALLET);
        when(traceAggregationMapper.selectActiveParentsByChild("TRACE-001"))
                .thenReturn(List.of(relation(1L, "CARTON-001", "TRACE-001", TraceAggregationRelationType.CARTON, true)));

        assertThatThrownBy(() -> service.bindChild(request, 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.CONFLICT));
    }

    @Test
    void releaseRelation_shouldDeactivateExistingRelation() {
        TraceAggregation relation = relation(9L, "CARTON-001", "TRACE-001", TraceAggregationRelationType.CARTON, true);
        when(traceAggregationMapper.selectById(9L)).thenReturn(relation);
        when(traceSnapshotMapper.selectById("TRACE-001")).thenReturn(snapshot("TRACE-001", "IN_STOCK"));
        when(traceLogFactory.createLog(
                eq("TRACE-001"), eq(1L), eq(ActionType.UNPACK), eq("CARTON-001"), eq("北京仓"),
                eq("北京"), eq("北京市"), any(), any(), any(), eq("hash-0"), any(), eq("operator-a")
        )).thenAnswer(invocation -> lifecycleLog(
                502L,
                "unpack-hash",
                ((ActionType) invocation.getArgument(2)).getCode(),
                invocation.getArgument(4)
        ));
        TraceAggregationReleaseRequest request = new TraceAggregationReleaseRequest();
        request.setRemark("拆箱复核");

        TraceAggregationResponse response = service.releaseRelation(9L, request, 7L, "operator-a");

        assertThat(relation.getActive()).isFalse();
        assertThat(relation.getReleaseTime()).isNotNull();
        assertThat(relation.getRemark()).isEqualTo("拆箱复核");
        assertThat(response.getActive()).isFalse();
        verify(traceAggregationMapper).updateById(relation);
        ArgumentCaptor<TraceLifecycleLog> logCaptor = ArgumentCaptor.forClass(TraceLifecycleLog.class);
        verify(traceLifecycleLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo(ActionType.UNPACK.getCode());
        verify(traceSnapshotMapper).updateById(any(TraceSnapshot.class));
    }

    @Test
    void bindChild_shouldRejectPackagingWhenSingleCodeInTransit() {
        TraceAggregationBindRequest request = bindRequest("carton-001", "trace-001", TraceAggregationRelationType.CARTON);
        when(traceAggregationMapper.selectActiveRelation("CARTON-001", "TRACE-001")).thenReturn(null);
        when(traceAggregationMapper.selectActiveParentsByChild("TRACE-001")).thenReturn(List.of());
        when(traceCodeMapper.selectByTraceCode("TRACE-001")).thenReturn(traceCode("TRACE-001"));
        when(traceSnapshotMapper.selectById("TRACE-001")).thenReturn(snapshot("TRACE-001", "IN_TRANSIT"));

        assertThatThrownBy(() -> service.bindChild(request, 7L, "operator-a"))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(BizCode.INVALID_ACTION_TYPE));
    }

    @Test
    void bindChild_shouldWritePalletizeLogForDirectSingleCodePalletBinding() {
        TraceAggregationBindRequest request = bindRequest("pallet-001", "trace-001", TraceAggregationRelationType.PALLET);
        when(traceAggregationMapper.selectActiveRelation("PALLET-001", "TRACE-001")).thenReturn(null);
        when(traceAggregationMapper.selectActiveParentsByChild("TRACE-001")).thenReturn(List.of());
        when(traceCodeMapper.selectByTraceCode("TRACE-001")).thenReturn(traceCode("TRACE-001"));
        when(traceSnapshotMapper.selectById("TRACE-001")).thenReturn(snapshot("TRACE-001", "IN_STOCK"));
        when(traceLogFactory.createLog(
                eq("TRACE-001"), eq(1L), eq(ActionType.PALLETIZE), eq("北京仓"), eq("PALLET-001"),
                eq("北京"), eq("北京市"), any(), any(), any(), eq("hash-0"), any(), eq("operator-a")
        )).thenAnswer(invocation -> lifecycleLog(
                503L,
                "palletize-hash",
                ((ActionType) invocation.getArgument(2)).getCode(),
                invocation.getArgument(4)
        ));

        TraceAggregationResponse response = service.bindChild(request, 7L, "operator-a");

        assertThat(response.getRelationType()).isEqualTo(TraceAggregationRelationType.PALLET);
        verify(traceLifecycleLogMapper).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper).updateById(any(TraceSnapshot.class));
    }

    @Test
    void bindChild_shouldWritePalletizeLogsForCartonChildrenWhenBindingCartonToPallet() {
        TraceAggregationBindRequest request = bindRequest("pallet-001", "carton-001", TraceAggregationRelationType.PALLET);
        when(traceAggregationMapper.selectActiveRelation("PALLET-001", "CARTON-001")).thenReturn(null);
        when(traceAggregationMapper.selectActiveParentsByChild("CARTON-001")).thenReturn(List.of());
        when(traceAggregationMapper.selectActiveChildrenByParent("CARTON-001"))
                .thenReturn(List.of(
                        relation(11L, "CARTON-001", "TRACE-001", TraceAggregationRelationType.CARTON, true),
                        relation(12L, "CARTON-001", "TRACE-002", TraceAggregationRelationType.CARTON, true)
                ));
        when(traceSnapshotMapper.selectById("TRACE-001")).thenReturn(snapshot("TRACE-001", "IN_STOCK"));
        when(traceSnapshotMapper.selectById("TRACE-002")).thenReturn(snapshot("TRACE-002", "IN_STOCK"));
        when(traceLogFactory.createLog(
                any(), eq(1L), eq(ActionType.PALLETIZE), eq("北京仓"), eq("PALLET-001"),
                eq("北京"), eq("北京市"), any(), any(), any(), eq("hash-0"), any(), eq("operator-a")
        )).thenAnswer(invocation -> lifecycleLog(
                600L,
                "palletize-hash-" + invocation.getArgument(0),
                ((ActionType) invocation.getArgument(2)).getCode(),
                invocation.getArgument(4)
        ));

        service.bindChild(request, 7L, "operator-a");

        verify(traceLifecycleLogMapper, times(2)).insert(any(TraceLifecycleLog.class));
        verify(traceSnapshotMapper, times(2)).updateById(any(TraceSnapshot.class));
    }

    @Test
    void listActiveChildren_shouldNormalizeParentCodeAndReturnResponses() {
        when(traceAggregationMapper.selectActiveChildrenByParent("CARTON-001"))
                .thenReturn(List.of(relation(1L, "CARTON-001", "TRACE-001", TraceAggregationRelationType.CARTON, true)));

        List<TraceAggregationResponse> responses = service.listActiveChildren(" carton-001 ");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getParentCode()).isEqualTo("CARTON-001");
        assertThat(responses.get(0).getChildCode()).isEqualTo("TRACE-001");
    }

    private static TraceAggregationBindRequest bindRequest(
            String parentCode,
            String childCode,
            TraceAggregationRelationType relationType
    ) {
        TraceAggregationBindRequest request = new TraceAggregationBindRequest();
        request.setParentCode(parentCode);
        request.setChildCode(childCode);
        request.setRelationType(relationType);
        request.setRemark(" 装箱 ");
        return request;
    }

    private static TraceAggregation relation(
            Long id,
            String parentCode,
            String childCode,
            TraceAggregationRelationType relationType,
            boolean active
    ) {
        TraceAggregation relation = new TraceAggregation();
        relation.setId(id);
        relation.setParentCode(parentCode);
        relation.setChildCode(childCode);
        relation.setRelationType(relationType.getCode());
        relation.setActive(active);
        return relation;
    }

    private static TraceCode traceCode(String value) {
        TraceCode traceCode = new TraceCode();
        traceCode.setTraceCode(value);
        traceCode.setSpuId(1L);
        traceCode.setCodeStatus("ACTIVATED");
        return traceCode;
    }

    private static TraceSnapshot snapshot(String traceCode, String currentStatus) {
        TraceSnapshot snapshot = new TraceSnapshot();
        snapshot.setTraceCode(traceCode);
        snapshot.setSpuId(1L);
        snapshot.setCurrentStatus(currentStatus);
        snapshot.setCurrentNode("北京仓");
        snapshot.setProvince("北京");
        snapshot.setCity("北京市");
        snapshot.setLastHash("hash-0");
        return snapshot;
    }

    private static TraceLifecycleLog lifecycleLog(Long id, String hash, String actionType, String toNode) {
        TraceLifecycleLog log = new TraceLifecycleLog();
        log.setId(id);
        log.setActionType(actionType);
        log.setToNode(toNode);
        log.setCurrentHash(hash);
        return log;
    }
}
