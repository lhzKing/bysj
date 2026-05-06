package com.example.trace.security.permission;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trace.entity.SysPermission;
import com.example.trace.entity.SysRole;
import com.example.trace.mapper.SysPermissionMapper;
import com.example.trace.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads role-linked permission data from persistence.
 */
@Service
public class RolePermissionQueryService {

    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;

    public RolePermissionQueryService(SysPermissionMapper permissionMapper, SysRoleMapper roleMapper) {
        this.permissionMapper = permissionMapper;
        this.roleMapper = roleMapper;
    }

    public List<SysPermission> getPermissionsByRoleId(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        List<SysPermission> permissions = permissionMapper.selectByRoleId(roleId);
        return permissions != null ? permissions : List.of();
    }

    public Long getRoleIdByCode(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        SysRole role = roleMapper.selectOne(
            new QueryWrapper<SysRole>().eq("role_code", roleCode)
        );
        return role != null ? role.getId() : null;
    }
}
