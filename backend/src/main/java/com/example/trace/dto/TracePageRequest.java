package com.example.trace.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 溯源码列表分页查询请求。
 *
 * <p>支持按追溯码 keyword 模糊、按状态/SPU/批次/节点/持有方/省份精确过滤，
 * 以及 last_event_time 时间范围过滤。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TracePageRequest extends PageRequest {

    /**
     * 关键词：模糊匹配 trace_code、SPU 名称、SPU 编码、当前持有方。
     */
    private String keyword;

    /**
     * 状态精确匹配（INIT / IN_STOCK / IN_TRANSIT / TRANSFERRED / EXCEPTION）。
     * 多值用逗号分隔，例如 "IN_STOCK,IN_TRANSIT"。
     */
    private String status;

    private Long spuId;

    private String batchNo;

    private String currentNode;

    private String currentOwner;

    private String province;

    /**
     * last_event_time 起始（含），ISO-8601 例如 2026-05-01T00:00:00。
     */
    private String eventTimeFrom;

    /**
     * last_event_time 截止（含）。
     */
    private String eventTimeTo;
}
