package com.example.trace.dto;

import com.example.trace.enums.TraceAggregationRelationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量装箱 / 装托结果汇总。
 *
 * 「跳过失败继续」语义下，部分子码可能绑定成功、部分失败，因此返回逐项结果：
 *   - {@code succeeded} 是成功创建的聚合关系（含 id / bindTime）
 *   - {@code failed} 是被业务规则拒绝的子码及原因（如已在别的箱里、状态 IN_TRANSIT 等）
 * 调用方据此提示「成功 N / 失败 M」并对失败项给出重试入口。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceAggregationBatchBindResponse {

    private String parentCode;
    private TraceAggregationRelationType relationType;
    private String relationTypeLabel;

    /** 去重后实际处理的子码数量（= successCount + failureCount）。 */
    private int totalRequested;
    private int successCount;
    private int failureCount;

    private List<TraceAggregationResponse> succeeded;
    private List<FailedChild> failed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedChild {
        private String childCode;
        /** 业务错误码（见 {@link com.example.trace.common.BizCode}）。 */
        private int code;
        private String message;
    }
}
