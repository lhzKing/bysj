package com.example.trace.service;

import com.example.trace.dto.TraceAggregationBatchBindRequest;
import com.example.trace.dto.TraceAggregationBatchBindResponse;
import com.example.trace.dto.TraceAggregationBindRequest;
import com.example.trace.dto.TraceAggregationReleaseRequest;
import com.example.trace.dto.TraceAggregationResponse;

import java.util.List;

public interface TraceAggregationService {

    TraceAggregationResponse bindChild(
            TraceAggregationBindRequest request,
            Long operatorUserId,
            String operatorUsername
    );

    /**
     * 批量绑定：一个父码 + 多个子码，逐个独立事务提交，失败跳过不影响其他子码。
     */
    TraceAggregationBatchBindResponse bindChildrenBatch(
            TraceAggregationBatchBindRequest request,
            Long operatorUserId,
            String operatorUsername
    );

    TraceAggregationResponse releaseRelation(
            Long relationId,
            TraceAggregationReleaseRequest request,
            Long operatorUserId,
            String operatorUsername
    );

    List<TraceAggregationResponse> listActiveChildren(String parentCode);

    List<TraceAggregationResponse> listActiveParents(String childCode);

    List<TraceAggregationResponse> listHistoryByParent(String parentCode);

    List<TraceAggregationResponse> listHistoryByChild(String childCode);

    List<TraceAggregationResponse> listAllActive(String relationType);
}
