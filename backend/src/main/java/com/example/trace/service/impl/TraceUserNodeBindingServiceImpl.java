package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceUserNodeBindingResponse;
import com.example.trace.dto.TraceUserNodeBindingUpdateRequest;
import com.example.trace.entity.SysUser;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceUserNodeBinding;
import com.example.trace.enums.ActionType;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.mapper.TraceUserNodeBindingMapper;
import com.example.trace.service.TraceUserNodeBindingService;
import com.example.trace.validation.TraceLocationFieldConstraints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TraceUserNodeBindingServiceImpl implements TraceUserNodeBindingService {

    private final TraceUserNodeBindingMapper bindingMapper;
    private final TraceNodeMapper traceNodeMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<TraceUserNodeBindingResponse> listUserBindings(Long userId) {
        requireExistingUser(userId);
        return toResponses(selectBindings(userId, null));
    }

    @Override
    @Transactional
    public List<TraceUserNodeBindingResponse> replaceUserBindings(
            Long userId,
            TraceUserNodeBindingUpdateRequest request
    ) {
        requireExistingUser(userId);
        if (request == null) {
            throw new BizException(BizCode.PARAM_ERROR, "节点绑定参数不能为空");
        }
        if (request.getNodeIds() == null) {
            throw new BizException(BizCode.PARAM_ERROR, "nodeIds 不能为空");
        }

        List<Long> nodeIds = normalizeNodeIds(request.getNodeIds());
        Long defaultNodeId = request.getDefaultNodeId();
        if (defaultNodeId != null && !nodeIds.contains(defaultNodeId)) {
            throw new BizException(BizCode.PARAM_ERROR, "defaultNodeId 必须包含在 nodeIds 中");
        }
        if (defaultNodeId == null && nodeIds.size() == 1) {
            defaultNodeId = nodeIds.get(0);
        }

        Map<Long, TraceNode> nodesById = loadNodesForBinding(nodeIds);
        bindingMapper.delete(new LambdaQueryWrapper<TraceUserNodeBinding>()
                .eq(TraceUserNodeBinding::getUserId, userId));

        for (Long nodeId : nodeIds) {
            TraceNode node = nodesById.get(nodeId);
            TraceUserNodeBinding binding = new TraceUserNodeBinding();
            binding.setUserId(userId);
            binding.setNodeId(nodeId);
            binding.setOrgId(node.getOrgId());
            binding.setDefaultNode(Objects.equals(nodeId, defaultNodeId));
            binding.setEnabled(true);
            bindingMapper.insert(binding);
        }
        return listUserBindings(userId);
    }

    @Override
    public List<TraceNode> listEnabledOperationNodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<TraceUserNodeBinding> bindings = selectBindings(userId, true);
        if (bindings.isEmpty()) {
            return List.of();
        }
        List<Long> nodeIds = bindings.stream()
                .map(TraceUserNodeBinding::getNodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (nodeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, TraceUserNodeBinding> bindingByNodeId = bindings.stream()
                .collect(Collectors.toMap(
                        TraceUserNodeBinding::getNodeId,
                        Function.identity(),
                        (left, right) -> Boolean.TRUE.equals(left.getDefaultNode()) ? left : right,
                        HashMap::new
                ));
        return traceNodeMapper.selectBatchIds(nodeIds).stream()
                .filter(node -> Boolean.TRUE.equals(node.getEnabled()))
                .sorted((left, right) -> compareByDefaultThenName(left, right, bindingByNodeId))
                .toList();
    }

    @Override
    public RouteResolution authorizeAndResolveRoute(
            Long userId,
            ActionType actionType,
            String fromNode,
            String toNode
    ) {
        if (userId == null || !requiresNodeAuthorization(actionType)) {
            return new RouteResolution(fromNode, toNode, null);
        }

        List<TraceNode> nodes = listEnabledOperationNodes(userId);
        if (nodes.isEmpty()) {
            throw new BizException(BizCode.FORBIDDEN, "当前用户未绑定任何可操作节点");
        }

        return switch (actionType) {
            case INBOUND -> authorizeInbound(userId, nodes, fromNode, toNode);
            case OUTBOUND, TRANSFER, EXCEPTION -> authorizeSourceNode(userId, nodes, actionType, fromNode, toNode);
            default -> new RouteResolution(fromNode, toNode, null);
        };
    }

    @Override
    public boolean canExecuteActionAtCurrentNode(Long userId, ActionType actionType, String currentNode) {
        if (userId == null || !requiresNodeAuthorization(actionType)) {
            return true;
        }
        List<TraceNode> nodes = listEnabledOperationNodes(userId);
        if (nodes.isEmpty()) {
            return false;
        }
        if (actionType == ActionType.INBOUND) {
            return true;
        }
        String normalizedCurrentNode = normalizeNodeOrNull("currentNode", currentNode);
        if (!StringUtils.hasText(normalizedCurrentNode)) {
            return true;
        }
        return findMatchedNode(nodes, normalizedCurrentNode) != null;
    }

    private RouteResolution authorizeInbound(Long userId, List<TraceNode> nodes, String fromNode, String toNode) {
        String normalizedToNode = normalizeNodeOrNull("toNode", toNode);
        TraceNode operationNode;
        if (StringUtils.hasText(normalizedToNode)) {
            operationNode = requireMatchedNode(nodes, normalizedToNode, "目标节点");
        } else {
            operationNode = requireDefaultOrOnlyNode(userId, nodes, "INBOUND 未指定 toNode 时需要用户存在唯一或默认节点");
            normalizedToNode = operationNode.getNodeName();
        }
        return new RouteResolution(fromNode, normalizedToNode, operationNode);
    }

    private RouteResolution authorizeSourceNode(
            Long userId,
            List<TraceNode> nodes,
            ActionType actionType,
            String fromNode,
            String toNode
    ) {
        String normalizedFromNode = normalizeNodeOrNull("fromNode", fromNode);
        TraceNode operationNode;
        if (StringUtils.hasText(normalizedFromNode)) {
            operationNode = requireMatchedNode(nodes, normalizedFromNode, "来源节点");
        } else {
            operationNode = requireDefaultOrOnlyNode(userId, nodes,
                    actionType.getCode() + " 未指定 fromNode 时需要用户存在唯一或默认节点");
            normalizedFromNode = operationNode.getNodeName();
        }
        return new RouteResolution(normalizedFromNode, toNode, operationNode);
    }

    private TraceNode requireMatchedNode(List<TraceNode> nodes, String requestedNode, String label) {
        TraceNode matched = findMatchedNode(nodes, requestedNode);
        if (matched == null) {
            throw new BizException(BizCode.FORBIDDEN,
                    "当前用户未绑定" + label + ": " + requestedNode);
        }
        return matched;
    }

    private TraceNode findMatchedNode(List<TraceNode> nodes, String requestedNode) {
        if (!StringUtils.hasText(requestedNode)) {
            return null;
        }
        String normalizedRequested = requestedNode.trim();
        for (TraceNode node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.getNodeName() != null && node.getNodeName().equals(normalizedRequested)) {
                return node;
            }
            if (node.getNodeCode() != null && node.getNodeCode().equalsIgnoreCase(normalizedRequested)) {
                return node;
            }
        }
        return null;
    }

    private TraceNode requireDefaultOrOnlyNode(Long userId, List<TraceNode> nodes, String message) {
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        List<TraceUserNodeBinding> enabledBindings = selectBindingsForNodes(userId, nodes);
        Set<Long> defaultNodeIds = enabledBindings.stream()
                .filter(binding -> Boolean.TRUE.equals(binding.getDefaultNode()))
                .map(TraceUserNodeBinding::getNodeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<TraceNode> defaults = nodes.stream()
                .filter(node -> defaultNodeIds.contains(node.getId()))
                .toList();
        if (defaults.size() == 1) {
            return defaults.get(0);
        }
        throw new BizException(BizCode.PARAM_ERROR, message);
    }

    private List<TraceUserNodeBinding> selectBindingsForNodes(Long userId, List<TraceNode> nodes) {
        List<Long> nodeIds = nodes.stream().map(TraceNode::getId).toList();
        if (nodeIds.isEmpty()) {
            return List.of();
        }
        return bindingMapper.selectList(new LambdaQueryWrapper<TraceUserNodeBinding>()
                .eq(TraceUserNodeBinding::getUserId, userId)
                .in(TraceUserNodeBinding::getNodeId, nodeIds)
                .eq(TraceUserNodeBinding::getEnabled, true));
    }

    private boolean requiresNodeAuthorization(ActionType actionType) {
        return actionType == ActionType.INBOUND
                || actionType == ActionType.OUTBOUND
                || actionType == ActionType.TRANSFER
                || actionType == ActionType.EXCEPTION;
    }

    private int compareByDefaultThenName(
            TraceNode left,
            TraceNode right,
            Map<Long, TraceUserNodeBinding> bindingByNodeId
    ) {
        TraceUserNodeBinding leftBinding = bindingByNodeId.get(left.getId());
        TraceUserNodeBinding rightBinding = bindingByNodeId.get(right.getId());
        boolean leftDefault = leftBinding != null && Boolean.TRUE.equals(leftBinding.getDefaultNode());
        boolean rightDefault = rightBinding != null && Boolean.TRUE.equals(rightBinding.getDefaultNode());
        if (leftDefault != rightDefault) {
            return leftDefault ? -1 : 1;
        }
        String leftName = left.getNodeName() == null ? "" : left.getNodeName();
        String rightName = right.getNodeName() == null ? "" : right.getNodeName();
        return leftName.compareTo(rightName);
    }

    private List<Long> normalizeNodeIds(List<Long> rawNodeIds) {
        Set<Long> unique = new LinkedHashSet<>();
        for (Long nodeId : rawNodeIds) {
            if (nodeId == null) {
                throw new BizException(BizCode.PARAM_ERROR, "nodeIds 不能包含空值");
            }
            if (nodeId <= 0) {
                throw new BizException(BizCode.PARAM_ERROR, "nodeId 必须为正数: " + nodeId);
            }
            unique.add(nodeId);
        }
        return new ArrayList<>(unique);
    }

    private Map<Long, TraceNode> loadNodesForBinding(List<Long> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TraceNode> nodes = traceNodeMapper.selectBatchIds(nodeIds);
        Map<Long, TraceNode> nodesById = nodes.stream()
                .collect(Collectors.toMap(TraceNode::getId, Function.identity()));
        for (Long nodeId : nodeIds) {
            TraceNode node = nodesById.get(nodeId);
            if (node == null) {
                throw new BizException(BizCode.NOT_FOUND, "节点不存在: " + nodeId);
            }
            if (!Boolean.TRUE.equals(node.getEnabled())) {
                throw new BizException(BizCode.BAD_REQUEST, "不能绑定已停用节点: " + nodeId);
            }
        }
        return nodesById;
    }

    private List<TraceUserNodeBinding> selectBindings(Long userId, Boolean enabled) {
        LambdaQueryWrapper<TraceUserNodeBinding> wrapper = new LambdaQueryWrapper<TraceUserNodeBinding>()
                .eq(TraceUserNodeBinding::getUserId, userId);
        if (enabled != null) {
            wrapper.eq(TraceUserNodeBinding::getEnabled, enabled);
        }
        wrapper.orderByDesc(TraceUserNodeBinding::getDefaultNode)
                .orderByAsc(TraceUserNodeBinding::getId);
        return bindingMapper.selectList(wrapper);
    }

    private SysUser requireExistingUser(Long userId) {
        if (userId == null) {
            throw new BizException(BizCode.PARAM_ERROR, "userId 不能为空");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(BizCode.NOT_FOUND, "用户不存在: " + userId);
        }
        return user;
    }

    private List<TraceUserNodeBindingResponse> toResponses(List<TraceUserNodeBinding> bindings) {
        if (bindings.isEmpty()) {
            return List.of();
        }
        List<Long> nodeIds = bindings.stream()
                .map(TraceUserNodeBinding::getNodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, TraceNode> nodesById = nodeIds.isEmpty()
                ? Map.of()
                : traceNodeMapper.selectBatchIds(nodeIds).stream()
                        .collect(Collectors.toMap(TraceNode::getId, Function.identity()));
        return bindings.stream()
                .map(binding -> toResponse(binding, nodesById.get(binding.getNodeId())))
                .toList();
    }

    private TraceUserNodeBindingResponse toResponse(TraceUserNodeBinding binding, TraceNode node) {
        return TraceUserNodeBindingResponse.builder()
                .bindingId(binding.getId())
                .userId(binding.getUserId())
                .nodeId(binding.getNodeId())
                .nodeCode(node == null ? null : node.getNodeCode())
                .nodeName(node == null ? null : node.getNodeName())
                .nodeType(node == null ? null : node.getNodeType())
                .orgId(binding.getOrgId())
                .province(node == null ? null : node.getProvince())
                .city(node == null ? null : node.getCity())
                .address(node == null ? null : node.getAddress())
                .nodeEnabled(node == null ? null : node.getEnabled())
                .bindingEnabled(binding.getEnabled())
                .defaultNode(binding.getDefaultNode())
                .createTime(binding.getCreateTime())
                .updateTime(binding.getUpdateTime())
                .build();
    }

    private String normalizeNodeOrNull(String fieldName, String value) {
        return TraceLocationFieldConstraints.normalizeNode(fieldName, value);
    }
}
