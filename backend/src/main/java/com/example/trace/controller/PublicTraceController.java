package com.example.trace.controller;

import com.example.trace.common.ApiResponse;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.dto.PartResponse;
import com.example.trace.dto.PublicTraceResponse;
import com.example.trace.dto.TraceDetailResponse;
import com.example.trace.entity.TraceLifecycleLog;
import com.example.trace.entity.TraceSnapshot;
import com.example.trace.enums.ActionType;
import com.example.trace.service.PartService;
import com.example.trace.service.TraceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公开追溯查验入口 — 不需要登录，面向消费者扫码自助验签场景。
 *
 * <p>区别于 {@link TraceController#getTrace(String, String, jakarta.servlet.http.HttpServletRequest)}：
 * 该控制器返回 {@link PublicTraceResponse} 已剔除操作员姓名、内部节点 ID/名称、内部备注、
 * prevHash/currentHash 详细比对数据等敏感字段，仅保留产品信息、状态、省份/城市级位置、
 * 事件动作可读名以及哈希链验签结果（valid + 计数 + anchorHash + 公钥）。</p>
 *
 * <p>该路径在 {@link com.example.trace.config.WebMvcConfig} 已加入 {@code LoginInterceptor}
 * 与 {@code PermissionInterceptor} 的 {@code excludePathPatterns} 白名单。</p>
 */
@RestController
@RequestMapping("/api/public/traces")
public class PublicTraceController {

    private final TraceService traceService;
    private final PartService partService;

    public PublicTraceController(TraceService traceService, PartService partService) {
        this.traceService = traceService;
        this.partService = partService;
    }

    @GetMapping("/{traceCode}")
    public ResponseEntity<ApiResponse<PublicTraceResponse>> getPublicTrace(@PathVariable String traceCode) {
        TraceDetailResponse detail = traceService.detail(traceCode, "effective", null);
        if (detail == null || detail.getSnapshot() == null) {
            throw new BizException(BizCode.TRACE_NOT_FOUND, "溯源码不存在");
        }

        TraceSnapshot snapshot = detail.getSnapshot();
        ChainVerifyResponse verify = traceService.verifyChain(traceCode);

        String spuName = null;
        if (snapshot.getSpuId() != null) {
            try {
                PartResponse part = partService.getPartById(snapshot.getSpuId());
                if (part != null) spuName = part.getPartName();
            } catch (Exception ignored) {
                // SPU may have been removed; leave name null rather than 500
            }
        }

        List<PublicTraceResponse.PublicEvent> events = detail.getHistory() == null ? List.of() :
                detail.getHistory().stream()
                        .sorted(Comparator.comparing(TraceLifecycleLog::getEventTime,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toPublicEvent)
                        .collect(Collectors.toList());

        PublicTraceResponse response = PublicTraceResponse.builder()
                .traceCode(snapshot.getTraceCode())
                .spuId(snapshot.getSpuId())
                .spuName(spuName)
                .currentStatus(snapshot.getCurrentStatus())
                .currentProvince(snapshot.getProvince())
                .currentCity(snapshot.getCity())
                .lastUpdateTime(snapshot.getLastEventTime() != null
                        ? snapshot.getLastEventTime() : snapshot.getUpdateTime())
                .totalEvents(events.size())
                .events(events)
                .chainVerify(verify)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private PublicTraceResponse.PublicEvent toPublicEvent(TraceLifecycleLog log) {
        String label = log.getActionType();
        if (ActionType.isValid(log.getActionType())) {
            label = ActionType.fromString(log.getActionType()).getName();
        }
        return PublicTraceResponse.PublicEvent.builder()
                .eventTime(log.getEventTime())
                .actionLabel(label)
                .province(log.getProvince())
                .city(log.getCity())
                .build();
    }
}
