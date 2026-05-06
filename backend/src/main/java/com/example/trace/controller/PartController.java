package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.*;
import com.example.trace.service.PartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配件管理控制器
 * 
 * 权限要求：part:view（查看）、part:manage（管理）
 */
@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;

    /**
     * 分页查询配件列表
     */
    @GetMapping
    @RequirePermission("part:view")
    public ApiResponse<PageResponse<PartResponse>> listParts(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "part_code", required = false) String partCode,
            @RequestParam(name = "part_name", required = false) String partName,
            @RequestParam(name = "part_type", required = false) String partType,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String order) {
        PartListRequest request = new PartListRequest();
        request.setKeyword(keyword);
        request.setPartCode(partCode);
        request.setPartName(partName);
        request.setPartType(partType);
        request.setManufacturer(manufacturer);
        request.setPage(page);
        request.setSize(size);
        request.setSort(sort);
        request.setOrder(order);
        return ApiResponse.success(partService.listParts(request));
    }

    /**
     * 获取配件详情
     */
    @GetMapping("/{id}")
    @RequirePermission("part:view")
    public ApiResponse<PartResponse> getPartById(@PathVariable Long id) {
        return ApiResponse.success(partService.getPartById(id));
    }

    /**
     * 根据配件编码获取配件
     */
    @GetMapping("/code/{partCode}")
    @RequirePermission("part:view")
    public ApiResponse<PartResponse> getPartByCode(@PathVariable String partCode) {
        return ApiResponse.success(partService.getPartByCode(partCode));
    }

    /**
     * 创建配件
     */
    @PostMapping
    @RequirePermission("part:manage")
    public ApiResponse<PartResponse> createPart(@Valid @RequestBody PartCreateRequest request) {
        return ApiResponse.success(partService.createPart(request));
    }

    /**
     * 更新配件
     */
    @PutMapping("/{id}")
    @RequirePermission("part:manage")
    public ApiResponse<PartResponse> updatePart(
            @PathVariable Long id,
            @Valid @RequestBody PartUpdateRequest request) {
        return ApiResponse.success(partService.updatePart(id, request));
    }

    /**
     * 删除配件
     */
    @DeleteMapping("/{id}")
    @RequirePermission("part:manage")
    public ApiResponse<Void> deletePart(@PathVariable Long id) {
        partService.deletePart(id);
        return ApiResponse.success(null);
    }

    /**
     * 批量删除配件
     */
    @DeleteMapping("/batch")
    @RequirePermission("part:manage")
    public ApiResponse<Integer> batchDeleteParts(@Valid @RequestBody BatchDeleteRequest request) {
        int count = partService.batchDelete(request.getIds());
        return ApiResponse.success(count);
    }

    /**
     * 获取所有配件类型（用于下拉选择）
     */
    @GetMapping("/types")
    @RequirePermission("part:view")
    public ApiResponse<List<String>> listPartTypes() {
        return ApiResponse.success(partService.listPartTypes());
    }

    /**
     * 获取所有厂商（用于下拉选择）
     */
    @GetMapping("/manufacturers")
    @RequirePermission("part:view")
    public ApiResponse<List<String>> listManufacturers() {
        return ApiResponse.success(partService.listManufacturers());
    }
}
