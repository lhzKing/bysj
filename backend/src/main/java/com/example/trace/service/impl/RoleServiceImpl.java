package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.PermissionResponse;
import com.example.trace.dto.RoleCreateRequest;
import com.example.trace.dto.RoleResponse;
import com.example.trace.dto.RoleUpdateRequest;
import com.example.trace.entity.SysPermission;
import com.example.trace.entity.SysRole;
import com.example.trace.entity.SysUser;
import com.example.trace.mapper.SysPermissionMapper;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysRolePermissionMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.security.PermissionService;
import com.example.trace.service.RoleService;
import com.example.trace.service.policy.RolePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Role management service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserMapper userMapper;
    private final PermissionService permissionService;
    private final RolePolicy rolePolicy;

    @Override
    public List<RoleResponse> listRoles() {
        List<SysRole> roles = roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)
        );
        Map<Long, Integer> permissionCountMap = buildPermissionCountMap(roles);
        return roles.stream()
            .map(role -> {
                RoleResponse response = convertToResponse(role);
                response.setPermissionCount(permissionCountMap.getOrDefault(role.getId(), 0));
                return response;
            })
            .collect(Collectors.toList());
    }

    @Override
    public RoleResponse getRoleById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(BizCode.NOT_FOUND, "Role not found");
        }

        RoleResponse response = convertToResponse(role);
        List<SysPermission> permissions = permissionMapper.selectByRoleId(id);
        response.setPermissions(permissions.stream()
            .map(this::convertToPermissionResponse)
            .collect(Collectors.toList()));
        response.setPermissionCount(permissions.size());
        return response;
    }

    @Override
    @Transactional
    public RoleResponse createRole(RoleCreateRequest request, String operatorRoleCode) {
        ensureOperatorRoleContext(operatorRoleCode);

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, request.getRoleCode());
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.CONFLICT, "Role code already exists");
        }

        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setRemark(request.getRemark());

        roleMapper.insert(role);
        log.info("Created role successfully: roleCode={}", role.getRoleCode());
        return getRoleById(role.getId());
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, RoleUpdateRequest request, String operatorRoleCode) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(BizCode.NOT_FOUND, "Role not found");
        }

        ensureCanManageRole(operatorRoleCode, role, "update");

        if (StringUtils.hasText(request.getRoleName())) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getRemark() != null) {
            role.setRemark(request.getRemark());
        }

        roleMapper.updateById(role);
        log.info("Updated role successfully: id={}, roleCode={}", id, role.getRoleCode());
        return getRoleById(id);
    }

    @Override
    @Transactional
    public void deleteRole(Long id, String operatorRoleCode) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(BizCode.NOT_FOUND, "Role not found");
        }

        ensureCanManageRole(operatorRoleCode, role, "delete");

        if (rolePolicy.isSystemRole(role.getRoleCode())) {
            throw new BizException(BizCode.FORBIDDEN, "System roles cannot be deleted");
        }

        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getRoleId, id);
        if (userMapper.selectCount(userWrapper) > 0) {
            throw new BizException(BizCode.CONFLICT, "Role is bound to users and cannot be deleted");
        }

        rolePermissionMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
        permissionService.clearCache();

        log.info("Deleted role successfully: id={}, roleCode={}", id, role.getRoleCode());
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(Long roleId, List<Long> permissionIds, String operatorRoleCode) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(BizCode.NOT_FOUND, "Role not found");
        }

        ensureCanManageRole(operatorRoleCode, role, "assign permissions");

        List<SysPermission> selectedPermissions = List.of();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            selectedPermissions = permissionMapper.selectBatchIds(permissionIds);
            if (selectedPermissions.size() != permissionIds.size()) {
                throw new BizException(BizCode.BAD_REQUEST, "Invalid permission ID");
            }

            ensureCanAssignProtectedPermissions(operatorRoleCode, selectedPermissions);
        }

        rolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            rolePermissionMapper.batchInsert(roleId, permissionIds);
        }

        permissionService.clearCache();
        log.info("Assigned role permissions successfully: roleId={}, permissionCount={}",
            roleId, permissionIds != null ? permissionIds.size() : 0);
        return getRoleById(roleId);
    }

    @Override
    public List<PermissionResponse> listPermissions() {
        List<SysPermission> permissions = permissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getId)
        );
        return permissions.stream()
            .map(this::convertToPermissionResponse)
            .collect(Collectors.toList());
    }

    private void ensureOperatorRoleContext(String operatorRoleCode) {
        rolePolicy.ensureOperatorRoleContext(operatorRoleCode, "role management");
    }

    private void ensureCanManageRole(String operatorRoleCode, SysRole targetRole, String action) {
        ensureOperatorRoleContext(operatorRoleCode);

        if (!rolePolicy.canManageRole(operatorRoleCode, targetRole.getRoleCode())) {
            throw new BizException(
                BizCode.FORBIDDEN,
                "No permission to " + action + " role: " + targetRole.getRoleCode()
            );
        }
    }

    private void ensureCanAssignProtectedPermissions(String operatorRoleCode, List<SysPermission> permissions) {
        ensureOperatorRoleContext(operatorRoleCode);
        if (rolePolicy.canAssignProtectedPermissions(operatorRoleCode) || permissions == null || permissions.isEmpty()) {
            return;
        }

        List<String> protectedPermissions = rolePolicy.protectedPermissionCodes(permissions.stream()
            .map(SysPermission::getPermCode)
            .toList());
        if (!protectedPermissions.isEmpty()) {
            throw new BizException(
                BizCode.FORBIDDEN,
                "Only SUPER_ADMIN can assign user/role management permissions: "
                    + String.join(", ", protectedPermissions)
            );
        }
    }

    private Map<Long, Integer> buildPermissionCountMap(List<SysRole> roles) {
        if (roles.isEmpty()) {
            return Map.of();
        }

        List<Long> roleIds = roles.stream()
            .map(SysRole::getId)
            .collect(Collectors.toList());

        List<SysRolePermissionMapper.RolePermissionCount> permissionCounts =
            rolePermissionMapper.countPermissionByRoleIds(roleIds);
        if (permissionCounts == null || permissionCounts.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> permissionCountMap = new LinkedHashMap<>();
        for (SysRolePermissionMapper.RolePermissionCount permissionCount : permissionCounts) {
            permissionCountMap.put(permissionCount.getRoleId(), permissionCount.getPermissionCount());
        }
        return permissionCountMap;
    }

    private RoleResponse convertToResponse(SysRole role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setRemark(role.getRemark());
        response.setCreateTime(role.getCreateTime());
        return response;
    }

    private PermissionResponse convertToPermissionResponse(SysPermission perm) {
        PermissionResponse response = new PermissionResponse();
        response.setId(perm.getId());
        response.setPermCode(perm.getPermCode());
        response.setPermName(perm.getPermName());
        response.setApiMethod(perm.getApiMethod());
        response.setApiPattern(perm.getApiPattern());
        response.setRemark(perm.getRemark());
        response.setCreateTime(perm.getCreateTime());
        return response;
    }
}
