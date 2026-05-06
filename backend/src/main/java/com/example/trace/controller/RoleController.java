package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import com.example.trace.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 * 
 * 权限要求：role:view（查看）、role:manage（管理）
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 查询所有角色
     */
    @GetMapping
    @RequirePermission("role:view")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(roleService.listRoles());
    }

    /**
     * 获取角色详情（包含权限列表）
     */
    @GetMapping("/{id}")
    @RequirePermission("role:view")
    public ApiResponse<RoleResponse> getRoleById(@PathVariable Long id) {
        return ApiResponse.success(roleService.getRoleById(id));
    }

    /**
     * 创建角色
     */
    @PostMapping
    @RequirePermission("role:manage")
    public ApiResponse<RoleResponse> createRole(
            HttpServletRequest request,
            @Valid @RequestBody RoleCreateRequest createRequest) {
        String operatorRole = (String) request.getAttribute("role");
        return ApiResponse.success(roleService.createRole(createRequest, operatorRole));
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @RequirePermission("role:manage")
    public ApiResponse<RoleResponse> updateRole(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest updateRequest) {
        String operatorRole = (String) request.getAttribute("role");
        return ApiResponse.success(roleService.updateRole(id, updateRequest, operatorRole));
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @RequirePermission("role:manage")
    public ApiResponse<Void> deleteRole(
            HttpServletRequest request,
            @PathVariable Long id) {
        String operatorRole = (String) request.getAttribute("role");
        roleService.deleteRole(id, operatorRole);
        return ApiResponse.success(null);
    }

    /**
     * 分配权限给角色
     */
    @PutMapping("/{id}/permissions")
    @RequirePermission("role:manage")
    public ApiResponse<RoleResponse> assignPermissions(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody AssignPermissionsRequest assignRequest) {
        String operatorRole = (String) request.getAttribute("role");
        return ApiResponse.success(roleService.assignPermissions(id, assignRequest.getPermissionIds(), operatorRole));
    }

    /**
     * 查询所有权限（用于权限分配界面）
     */
    @GetMapping("/permissions")
    @RequirePermission("role:view")
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.success(roleService.listPermissions());
    }
}
