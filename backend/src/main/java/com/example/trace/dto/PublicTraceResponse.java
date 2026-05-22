package com.example.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 匿名公开的追溯码查验响应。
 *
 * <p>面向消费者「拿到产品后扫码自助验签」场景，绕过登录但只暴露
 * 公开安全的字段：去掉了操作员姓名、内部节点 ID/名称、内部备注，
 * 仅保留产品信息、状态、省份/城市级位置、事件类型与哈希链验签结果。</p>
 *
 * <p>该 DTO 由 {@code com.example.trace.controller.PublicTraceController} 返回，
 * 不需要 token，路径 {@code /api/public/traces/**} 已在 {@code WebMvcConfig}
 * 拦截器白名单中。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicTraceResponse {

    private String traceCode;
    private Long spuId;
    private String spuName;

    /** 当前业务状态 (INIT / IN_STOCK / IN_TRANSIT / TRANSFERRED / EXCEPTION). */
    private String currentStatus;

    /** 当前所在省份/城市 (粗粒度位置，匿名安全). */
    private String currentProvince;
    private String currentCity;

    private LocalDateTime lastUpdateTime;
    private int totalEvents;

    private List<PublicEvent> events;
    private ChainVerifyResponse chainVerify;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicEvent {
        private LocalDateTime eventTime;
        /** 操作动作的对外可读名称 (生产赋码 / 扫码激活 / 入库 / 出库 / 流转 / ...). */
        private String actionLabel;
        private String province;
        private String city;
    }
}
