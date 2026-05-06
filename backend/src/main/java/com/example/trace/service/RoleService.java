package com.example.trace.service;

import com.example.trace.dto.*;

import java.util.List;

/**
 * 角色管理服务接口
 */
public interface RoleService {

    /**
     * 查询所有角色
     */
    List<RoleResponse> listRoles();

    /**
     * 获取角色详情（包含权限列表）
     */
    RoleResponse getRoleById(Long id);

    /**
     * 创建角色
     */
    RoleResponse createRole(RoleCreateRequest request, String operatorRoleCode);

    /**
     * 更新角色
     */
    RoleResponse updateRole(Long id, RoleUpdateRequest request, String operatorRoleCode);

    /**
     * 删除角色
     */
    void deleteRole(Long id, String operatorRoleCode);

    /**
     * 分配权限给角色
     */
    RoleResponse assignPermissions(Long roleId, List<Long> permissionIds, String operatorRoleCode);

    /**
     * 查询所有权限
     */
    List<PermissionResponse> listPermissions();
}
