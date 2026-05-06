package com.example.trace.controller;

import com.example.trace.annotation.RequirePermission;
import com.example.trace.common.ApiResponse;
import com.example.trace.dto.*;
import com.example.trace.service.UserService;
import com.example.trace.service.TraceUserNodeBindingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 * 
 * 权限要求：user:view（查看）、user:manage（管理）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final String ATTR_USER_ID = "userId";

    private final UserService userService;
    private final TraceUserNodeBindingService traceUserNodeBindingService;

    /**
     * 分页查询用户列表
     * SUPER_ADMIN 可查看所有用户，ADMIN 只能查看 USER 角色用户
     */
    @GetMapping
    @RequirePermission("user:view")
    public ApiResponse<PageResponse<UserResponse>> listUsers(
            HttpServletRequest request,
            @RequestParam(required = false) String username,
            @RequestParam(name = "role_id", required = false) Long roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String order) {
        UserListRequest listRequest = new UserListRequest();
        listRequest.setUsername(username);
        listRequest.setRoleId(roleId);
        listRequest.setStatus(status);
        listRequest.setPage(page);
        listRequest.setSize(size);
        listRequest.setSort(sort);
        listRequest.setOrder(order);
        String operatorRole = (String) request.getAttribute("role");
        return ApiResponse.success(userService.listUsers(listRequest, operatorRole));
    }


    /**
     * 当前登录用户自己的可操作节点。
     */
    @GetMapping("/me/trace-nodes")
    @RequirePermission("trace:view")
    public ApiResponse<List<TraceUserNodeBindingResponse>> listMyTraceNodeBindings(
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute(ATTR_USER_ID);
        return ApiResponse.success(traceUserNodeBindingService.listUserBindings(userId));
    }

    /**
     * 管理员查看指定用户绑定的溯源业务节点。
     */
    @GetMapping("/{id}/trace-nodes")
    @RequirePermission("user:view")
    public ApiResponse<List<TraceUserNodeBindingResponse>> listUserTraceNodeBindings(
            @PathVariable Long id
    ) {
        return ApiResponse.success(traceUserNodeBindingService.listUserBindings(id));
    }

    /**
     * 管理员替换指定用户的溯源业务节点绑定关系。
     */
    @PutMapping("/{id}/trace-nodes")
    @RequirePermission("user:manage")
    public ApiResponse<List<TraceUserNodeBindingResponse>> replaceUserTraceNodeBindings(
            @PathVariable Long id,
            @Valid @RequestBody TraceUserNodeBindingUpdateRequest request
    ) {
        return ApiResponse.success(traceUserNodeBindingService.replaceUserBindings(id, request));
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @RequirePermission("user:view")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    /**
     * 创建用户
     */
    @PostMapping
    @RequirePermission("user:manage")
    public ApiResponse<UserResponse> createUser(
            HttpServletRequest request,
            @Valid @RequestBody UserCreateRequest createRequest) {
        String operatorRole = (String) request.getAttribute("role");
        return ApiResponse.success(userService.createUser(createRequest, operatorRole));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @RequirePermission("user:manage")
    public ApiResponse<UserResponse> updateUser(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest updateRequest) {
        String operatorRole = (String) request.getAttribute("role");
        return ApiResponse.success(userService.updateUser(id, updateRequest, operatorRole));
    }

    /**
     * 修改用户角色
     */
    @PatchMapping("/{id}/role")
    @RequirePermission("user:manage")
    public ApiResponse<UserResponse> changeUserRole(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam(name = "role_id") Long roleId) {
        String operatorRole = (String) request.getAttribute("role");
        return ApiResponse.success(userService.changeUserRole(id, roleId, operatorRole));
    }

    /**
     * 启用/禁用用户
     */
    @PatchMapping("/{id}/status")
    @RequirePermission("user:manage")
    public ApiResponse<UserResponse> toggleUserStatus(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam Integer status) {
        String operatorRole = (String) request.getAttribute("role");
        return ApiResponse.success(userService.toggleUserStatus(id, status, operatorRole));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @RequirePermission("user:manage")
    public ApiResponse<Void> deleteUser(
            HttpServletRequest request,
            @PathVariable Long id) {
        String operatorRole = (String) request.getAttribute("role");
        userService.deleteUser(id, operatorRole);
        return ApiResponse.success(null);
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @RequirePermission("user:manage")
    public ApiResponse<Void> resetPassword(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest passwordRequest) {
        // 获取操作者角色
        String operatorRole = (String) request.getAttribute("role");
        userService.resetPassword(id, passwordRequest.getNewPassword(), operatorRole);
        return ApiResponse.success(null);
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @RequirePermission("user:manage")
    public ApiResponse<Integer> batchDeleteUsers(
            HttpServletRequest request,
            @Valid @RequestBody BatchDeleteRequest deleteRequest) {
        String operatorRole = (String) request.getAttribute("role");
        int count = userService.batchDelete(deleteRequest.getIds(), operatorRole);
        return ApiResponse.success(count);
    }
}
