package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.TraceNodeCreateRequest;
import com.example.trace.dto.TraceNodeResponse;
import com.example.trace.dto.TraceNodeUpdateRequest;
import com.example.trace.enums.TraceNodeType;
import com.example.trace.service.TraceNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trace-nodes")
@RequiredArgsConstructor
public class TraceNodeController {

    private final TraceNodeService traceNodeService;

    @GetMapping
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceNodeResponse>> listNodes(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "node_type", required = false) TraceNodeType nodeType,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.success(traceNodeService.listNodes(keyword, nodeType, enabled));
    }

    @GetMapping("/selectable")
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceNodeResponse>> listSelectableNodes() {
        return ApiResponse.success(traceNodeService.listSelectableNodes());
    }

    @GetMapping("/{id}")
    @RequirePermission("trace:view")
    public ApiResponse<TraceNodeResponse> getNode(@PathVariable Long id) {
        return ApiResponse.success(traceNodeService.getNodeById(id));
    }

    @GetMapping("/code/{nodeCode}")
    @RequirePermission("trace:view")
    public ApiResponse<TraceNodeResponse> getNodeByCode(@PathVariable String nodeCode) {
        return ApiResponse.success(traceNodeService.getNodeByCode(nodeCode));
    }

    @PostMapping
    @RequirePermission("trace:create")
    public ApiResponse<TraceNodeResponse> createNode(@Valid @RequestBody TraceNodeCreateRequest request) {
        return ApiResponse.success(traceNodeService.createNode(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("trace:create")
    public ApiResponse<TraceNodeResponse> updateNode(
            @PathVariable Long id,
            @Valid @RequestBody TraceNodeUpdateRequest request
    ) {
        return ApiResponse.success(traceNodeService.updateNode(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("trace:create")
    public ApiResponse<Void> deleteNode(@PathVariable Long id) {
        traceNodeService.deleteNode(id);
        return ApiResponse.success(null);
    }
}
