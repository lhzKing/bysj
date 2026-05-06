package com.example.trace.service;

import com.example.trace.dto.*;

import java.util.List;

/**
 * 用户管理服务接口
 * 
 * 权限控制规则：
 * - SUPER_ADMIN 可以操作所有用户
 * - ADMIN 只能操作 USER 角色的用户
 * - 任何人都不能禁用/删除 superadmin 账号
 */
public interface UserService {

    /**
     * 分页查询用户列表
     * @param request 查询参数
     * @param operatorRoleCode 操作者角色代码，用于过滤可见用户
     */
    PageResponse<UserResponse> listUsers(UserListRequest request, String operatorRoleCode);

    /**
     * 根据ID获取用户详情
     */
    UserResponse getUserById(Long id);

    /**
     * 创建用户
     */
    UserResponse createUser(UserCreateRequest request, String operatorRoleCode);

    /**
     * 更新用户信息
     */
    UserResponse updateUser(Long id, UserUpdateRequest request, String operatorRoleCode);

    /**
     * 修改用户角色
     */
    UserResponse changeUserRole(Long id, Long roleId, String operatorRoleCode);

    /**
     * 启用/禁用用户
     */
    UserResponse toggleUserStatus(Long id, Integer status, String operatorRoleCode);

    /**
     * 删除用户
     */
    void deleteUser(Long id, String operatorRoleCode);

    /**
     * 批量删除用户
     */
    int batchDelete(List<Long> ids, String operatorRoleCode);

    /**
     * 重置用户密码（需要检查权限）
     * @param id 目标用户ID
     * @param newPassword 新密码
     * @param operatorRoleCode 操作者角色代码
     */
    void resetPassword(Long id, String newPassword, String operatorRoleCode);
}
