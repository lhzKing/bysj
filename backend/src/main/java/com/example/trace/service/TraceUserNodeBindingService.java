package com.example.trace.service;

import com.example.trace.dto.TraceUserNodeBindingResponse;
import com.example.trace.dto.TraceUserNodeBindingUpdateRequest;
import com.example.trace.entity.TraceNode;
import com.example.trace.enums.ActionType;

import java.util.List;

public interface TraceUserNodeBindingService {

    /*
     * 节点授权后的路线结果：
     * fromNode/toNode 是最终写入生命周期日志的路线节点；
     * operationNode 是本次动作实际落在哪个作业节点上，用于补齐省市等现场字段。
     */
    record RouteResolution(String fromNode, String toNode, TraceNode operationNode) {
    }

    /**
     * Lists all bindings for a user, including disabled nodes/bindings for admin visibility.
     *
     * <p>管理端需要看到“曾经绑定过但已禁用”的记录，便于审计和重新启用。</p>
     */
    List<TraceUserNodeBindingResponse> listUserBindings(Long userId);

    /**
     * Replaces the user's node bindings as one admin operation.
     *
     * <p>按一次管理操作整体替换，避免前端逐条增删导致中间态权限短暂不一致。</p>
     */
    List<TraceUserNodeBindingResponse> replaceUserBindings(
            Long userId,
            TraceUserNodeBindingUpdateRequest request
    );

    /**
     * Returns enabled binding nodes only; used by operation-time authorization.
     *
     * <p>扫码执行时只认启用状态的节点，禁用节点不会继续赋予现场作业权限。</p>
     */
    List<TraceNode> listEnabledOperationNodes(Long userId);

    /**
     * Authorizes a scan event against the current user's operable nodes and fills
     * the route node that can be inferred from the user's default/only binding.
     *
     * <p>这是五维权限过滤中“节点绑定”维度的核心入口：
     * 用户有接口权限和角色动作权限，不代表能在任意仓库/产线节点扫码。</p>
     */
    RouteResolution authorizeAndResolveRoute(
            Long userId,
            ActionType actionType,
            String fromNode,
            String toNode
    );

    /**
     * Lightweight predicate for available-action filtering before a task model exists.
     *
     * <p>用于扫码后“可执行动作”列表过滤，让前端只展示当前用户在当前节点真正能做的动作。</p>
     */
    boolean canExecuteActionAtCurrentNode(Long userId, ActionType actionType, String currentNode);
}
