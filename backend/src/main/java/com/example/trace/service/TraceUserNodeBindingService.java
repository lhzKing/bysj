package com.example.trace.service;

import com.example.trace.dto.TraceUserNodeBindingResponse;
import com.example.trace.dto.TraceUserNodeBindingUpdateRequest;
import com.example.trace.entity.TraceNode;
import com.example.trace.enums.ActionType;

import java.util.List;

public interface TraceUserNodeBindingService {

    record RouteResolution(String fromNode, String toNode, TraceNode operationNode) {
    }

    /**
     * Lists all bindings for a user, including disabled nodes/bindings for admin visibility.
     */
    List<TraceUserNodeBindingResponse> listUserBindings(Long userId);

    /**
     * Replaces the user's node bindings as one admin operation.
     */
    List<TraceUserNodeBindingResponse> replaceUserBindings(
            Long userId,
            TraceUserNodeBindingUpdateRequest request
    );

    /**
     * Returns enabled binding nodes only; used by operation-time authorization.
     */
    List<TraceNode> listEnabledOperationNodes(Long userId);

    /**
     * Authorizes a scan event against the current user's operable nodes and fills
     * the route node that can be inferred from the user's default/only binding.
     */
    RouteResolution authorizeAndResolveRoute(
            Long userId,
            ActionType actionType,
            String fromNode,
            String toNode
    );

    /**
     * Lightweight predicate for available-action filtering before a task model exists.
     */
    boolean canExecuteActionAtCurrentNode(Long userId, ActionType actionType, String currentNode);
}
