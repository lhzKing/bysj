package com.example.trace.service;

import com.example.trace.dto.ChainVerifyResponse;
import com.example.trace.dto.PageResponse;
import com.example.trace.dto.ProduceAssignRequest;
import com.example.trace.dto.ProduceAssignResponse;
import com.example.trace.dto.ScanTraceRequest;
import com.example.trace.dto.TraceAvailableActionsResponse;
import com.example.trace.dto.TraceCodeActivateRequest;
import com.example.trace.dto.TraceCodeActivateResponse;
import com.example.trace.dto.TraceCodeLabelActionRequest;
import com.example.trace.dto.TraceCodeLabelActionResponse;
import com.example.trace.dto.TraceCorrectionRequest;
import com.example.trace.dto.TraceDetailResponse;
import com.example.trace.dto.TraceExceptionCloseRequest;
import com.example.trace.dto.TraceListItemResponse;
import com.example.trace.dto.TracePageRequest;

public interface TraceService {

    /**
     * 生产赋码 - 创建溯源实例
     */
    ProduceAssignResponse produceAssign(ProduceAssignRequest request, String operator);

    /**
     * 分页查询追溯列表（trace_snapshot 视角，附 SPU/批次/最近动作聚合字段）。
     */
    PageResponse<TraceListItemResponse> listTraces(TracePageRequest request);

    /**
     * 扫码流转 - 记录流转事件
     */
    void scan(ScanTraceRequest request, String operator);

    /**
     * 标签打印 - 记录 PRINT_CODE 审计事件并更新单品码状态。
     */
    TraceCodeLabelActionResponse printCode(String traceCode, TraceCodeLabelActionRequest request, String operator);

    /**
     * 标签重打/补打 - 记录 REPRINT_CODE 审计事件。
     */
    TraceCodeLabelActionResponse reprintCode(String traceCode, TraceCodeLabelActionRequest request, String operator);

    /**
     * 标签作废 - 记录 VOID_CODE 审计事件并终止未激活码。
     */
    TraceCodeLabelActionResponse voidCode(String traceCode, TraceCodeLabelActionRequest request, String operator);

    /**
     * 单品码扫码激活/复核 - 记录 ACTIVATE_CODE 审计事件并允许后续真实流转。
     */
    TraceCodeActivateResponse activateCode(String traceCode, TraceCodeActivateRequest request, String operator);

    /**
     * 解除异常冻结，恢复到冻结前快照状态。
     */
    TraceCodeLabelActionResponse closeException(
            String traceCode,
            TraceExceptionCloseRequest request,
            Long operatorUserId,
            String operator
    );

    /**
     * 红冲蓝补式审计纠错：追加 CORRECTION 记录，不删除原日志。
     */
    TraceCodeLabelActionResponse correctLifecycleLog(
            String traceCode,
            TraceCorrectionRequest request,
            Long operatorUserId,
            String operator
    );

    /**
     * 溯源详情 - 查询溯源链。
     *
     * @param traceCode 溯源码
     * @param view effective=业务有效视图；audit=审计完整视图
     * @param roleId 当前登录角色ID，用于审计视图权限校验
     */
    TraceDetailResponse detail(String traceCode, String view, Long roleId);

    /**
     * 扫码后可执行动作 - 根据当前状态和角色权限返回推荐动作。
     */
    TraceAvailableActionsResponse availableActions(String traceCode, Long roleId);

    /**
     * 扫码后可执行动作 - B15 起加入当前用户节点绑定过滤。
     */
    TraceAvailableActionsResponse availableActions(String traceCode, Long roleId, Long userId);

    /**
     * 验证溯源链完整性
     * 包括 Hash 链连续性验证和数字签名验证
     * 
     * @param traceCode 溯源码
     * @return 验证结果，包含详细的错误信息
     */
    ChainVerifyResponse verifyChain(String traceCode);

    /**
     * 获取验证公钥
     * 供第三方验证者使用，验证数据真实性
     * 
     * @return Base64 编码的 RSA 公钥
     */
    String getPublicKey();

    /**
     * Current signature key id.
     */
    String getCurrentSignatureKeyId();

    /**
     * Current signature key version.
     */
    Integer getCurrentSignatureKeyVersion();
}
