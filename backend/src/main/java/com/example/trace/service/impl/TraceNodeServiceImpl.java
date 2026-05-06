package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.TraceNodeCreateRequest;
import com.example.trace.dto.TraceNodeResponse;
import com.example.trace.dto.TraceNodeUpdateRequest;
import com.example.trace.entity.TraceNode;
import com.example.trace.enums.TraceNodeType;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.service.TraceNodeService;
import com.example.trace.util.ProvinceUtil;
import com.example.trace.validation.TraceLocationFieldConstraints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TraceNodeServiceImpl implements TraceNodeService {

    private static final Pattern NODE_CODE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");

    private final TraceNodeMapper traceNodeMapper;

    @Override
    public List<TraceNodeResponse> listNodes(String keyword, TraceNodeType nodeType, Boolean enabled) {
        LambdaQueryWrapper<TraceNode> wrapper = new LambdaQueryWrapper<>();
        String normalizedKeyword = normalizeText(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            wrapper.and(w -> w
                    .like(TraceNode::getNodeCode, normalizedKeyword)
                    .or()
                    .like(TraceNode::getNodeName, normalizedKeyword)
                    .or()
                    .like(TraceNode::getAddress, normalizedKeyword)
            );
        }
        if (nodeType != null) {
            wrapper.eq(TraceNode::getNodeType, nodeType.name());
        }
        if (enabled != null) {
            wrapper.eq(TraceNode::getEnabled, enabled);
        }
        wrapper.orderByDesc(TraceNode::getEnabled)
                .orderByAsc(TraceNode::getNodeType)
                .orderByAsc(TraceNode::getNodeName);
        return traceNodeMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TraceNodeResponse> listSelectableNodes() {
        return listNodes(null, null, true);
    }

    @Override
    public TraceNodeResponse getNodeById(Long id) {
        return toResponse(requireNode(id));
    }

    @Override
    public TraceNodeResponse getNodeByCode(String nodeCode) {
        String normalizedCode = normalizeNodeCode(nodeCode);
        TraceNode node = traceNodeMapper.selectByNodeCode(normalizedCode);
        if (node == null) {
            throw new BizException(BizCode.NOT_FOUND, "节点不存在: " + normalizedCode);
        }
        return toResponse(node);
    }

    @Override
    @Transactional
    public TraceNodeResponse createNode(TraceNodeCreateRequest request) {
        if (request == null) {
            throw new BizException(BizCode.PARAM_ERROR, "节点创建参数不能为空");
        }
        String nodeCode = normalizeNodeCode(request.getNodeCode());
        if (traceNodeMapper.selectByNodeCode(nodeCode) != null) {
            throw new BizException(BizCode.CONFLICT, "节点编码已存在: " + nodeCode);
        }

        TraceNode node = new TraceNode();
        node.setNodeCode(nodeCode);
        node.setNodeName(normalizeRequiredNode("nodeName", request.getNodeName()));
        node.setNodeType(requireNodeType(request.getNodeType()).name());
        node.setOrgId(request.getOrgId());
        node.setProvince(normalizeRequiredRegion("province", ProvinceUtil.toFullName(request.getProvince())));
        node.setCity(normalizeRequiredRegion("city", request.getCity()));
        node.setAddress(normalizeAddress(request.getAddress()));
        node.setEnabled(request.getEnabled() == null || request.getEnabled());
        traceNodeMapper.insert(node);
        return toResponse(node);
    }

    @Override
    @Transactional
    public TraceNodeResponse updateNode(Long id, TraceNodeUpdateRequest request) {
        if (request == null) {
            throw new BizException(BizCode.PARAM_ERROR, "节点更新参数不能为空");
        }
        TraceNode node = requireNode(id);

        if (request.getNodeName() != null) {
            node.setNodeName(normalizeRequiredNode("nodeName", request.getNodeName()));
        }
        if (request.getNodeType() != null) {
            node.setNodeType(request.getNodeType().name());
        }
        if (request.getOrgId() != null) {
            node.setOrgId(request.getOrgId());
        }
        if (request.getProvince() != null) {
            node.setProvince(normalizeRequiredRegion("province", ProvinceUtil.toFullName(request.getProvince())));
        }
        if (request.getCity() != null) {
            node.setCity(normalizeRequiredRegion("city", request.getCity()));
        }
        if (request.getAddress() != null) {
            node.setAddress(normalizeAddress(request.getAddress()));
        }
        if (request.getEnabled() != null) {
            node.setEnabled(request.getEnabled());
        }
        traceNodeMapper.updateById(node);
        return toResponse(node);
    }

    @Override
    @Transactional
    public void deleteNode(Long id) {
        TraceNode node = requireNode(id);
        traceNodeMapper.deleteById(node.getId());
    }

    private TraceNode requireNode(Long id) {
        if (id == null) {
            throw new BizException(BizCode.PARAM_ERROR, "nodeId 不能为空");
        }
        TraceNode node = traceNodeMapper.selectById(id);
        if (node == null) {
            throw new BizException(BizCode.NOT_FOUND, "节点不存在: " + id);
        }
        return node;
    }

    private String normalizeNodeCode(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(BizCode.PARAM_ERROR, "nodeCode 不能为空");
        }
        if (normalized.length() > 64 || !NODE_CODE.matcher(normalized).matches()) {
            throw new BizException(BizCode.PARAM_ERROR, "nodeCode 格式不合法");
        }
        return normalized.toUpperCase();
    }

    private TraceNodeType requireNodeType(TraceNodeType nodeType) {
        if (nodeType == null) {
            throw new BizException(BizCode.PARAM_ERROR, "nodeType 不能为空");
        }
        return nodeType;
    }

    private String normalizeRequiredNode(String fieldName, String value) {
        String normalized = TraceLocationFieldConstraints.normalizeNode(fieldName, value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(BizCode.PARAM_ERROR, fieldName + " 不能为空");
        }
        return normalized;
    }

    private String normalizeRequiredRegion(String fieldName, String value) {
        String normalized = TraceLocationFieldConstraints.normalizeRegion(fieldName, value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(BizCode.PARAM_ERROR, fieldName + " 不能为空");
        }
        return normalized;
    }

    private String normalizeAddress(String value) {
        return TraceLocationFieldConstraints.normalizeRemark("address", value);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private TraceNodeResponse toResponse(TraceNode node) {
        return TraceNodeResponse.builder()
                .id(node.getId())
                .nodeCode(node.getNodeCode())
                .nodeName(node.getNodeName())
                .nodeType(node.getNodeType())
                .orgId(node.getOrgId())
                .province(node.getProvince())
                .city(node.getCity())
                .address(node.getAddress())
                .enabled(node.getEnabled())
                .createTime(node.getCreateTime())
                .updateTime(node.getUpdateTime())
                .build();
    }
}
