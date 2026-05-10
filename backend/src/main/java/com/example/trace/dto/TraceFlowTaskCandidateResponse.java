package com.example.trace.dto;

import com.example.trace.enums.ActionType;
import com.example.trace.enums.TraceFlowTaskStatus;
import com.example.trace.enums.TraceFlowTaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个追溯码可参与的开放运单候选项。
 * 用于普通扫码弹窗（ScanFlowDialog）展示运单下拉选择 + 自动填充 fromNode/toNode/省/市。
 *
 * <p>由 {@code TraceFlowTaskService.findCandidateFlowTasksForTrace} 计算：基于
 * {@code trace_snapshot.current_status / current_node} 和任务的 source/target 节点匹配，
 * 仅保留扫码后能产生有效 ScanPlan 的开放任务。</p>
 *
 * <p>前端拿到 candidate 后：
 * 1) 渲染 dropdown，label 为 {@code taskNo + '·' + taskTypeLabel + '·' + sourceNodeName + '→' + targetNodeName}；
 * 2) 选定后用 {@code prefillFromNode/prefillToNode/prefillProvince/prefillCity} 自动填弹窗字段；
 * 3) 提交时如果选了任务，路由到 {@code POST /api/trace-flow-tasks/{id}/scan} 而非 events 接口，
 *    这样能联动任务的 actualQuantity 与 status，接通"普通扫码 + 任务扫码"两条链路。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceFlowTaskCandidateResponse {

    private Long id;
    private String taskNo;
    private TraceFlowTaskType taskType;
    private String taskTypeLabel;
    private TraceFlowTaskStatus status;
    private String statusLabel;

    private String sourceNodeCode;
    private String sourceNodeName;
    private String targetNodeCode;
    private String targetNodeName;

    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer remainingQuantity;

    /**
     * 这个码在该任务下扫码会触发的动作类型（INBOUND / OUTBOUND）。
     * 由后端按 task_type + snapshot.currentStatus 派生，前端用于过滤"用户选定的扫码动作"是否匹配任务。
     */
    private ActionType compatibleActionType;

    private String prefillFromNode;
    private String prefillToNode;
    private String prefillProvince;
    private String prefillCity;
}
