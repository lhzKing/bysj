package com.example.trace.service;

import com.example.trace.dto.TraceNodeCreateRequest;
import com.example.trace.dto.TraceNodeResponse;
import com.example.trace.dto.TraceNodeUpdateRequest;
import com.example.trace.enums.TraceNodeType;

import java.util.List;

public interface TraceNodeService {

    List<TraceNodeResponse> listNodes(String keyword, TraceNodeType nodeType, Boolean enabled);

    List<TraceNodeResponse> listSelectableNodes();

    TraceNodeResponse getNodeById(Long id);

    TraceNodeResponse getNodeByCode(String nodeCode);

    TraceNodeResponse createNode(TraceNodeCreateRequest request);

    TraceNodeResponse updateNode(Long id, TraceNodeUpdateRequest request);

    void deleteNode(Long id);
}
