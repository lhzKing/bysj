package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trace.common.BizCode;
import com.example.trace.common.BizException;
import com.example.trace.dto.*;
import com.example.trace.entity.SysRole;
import com.example.trace.entity.SysUser;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.service.UserService;
import com.example.trace.security.PasswordEncoder;
import com.example.trace.service.policy.RolePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现
 * 
 * 权限控制规则：
 * - 角色层级、系统角色与保护账号规则统一由 {@link RolePolicy} 提供
 * - 只能操作优先级比自己低的用户
 * - superadmin 账号受特殊保护，任何人都不能禁用/删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final RolePolicy rolePolicy;

    /**
     * 检查操作权限
     * @param operatorRoleCode 操作者角色
     * @param targetRoleCode 目标用户角色
     * @param targetUsername 目标用户名（用于特殊保护检查）
     * @param action 操作描述
     */
    private void checkOperationPermission(String operatorRoleCode, String targetRoleCode, 
                                          String targetUsername, String action) {
        // 特殊保护：superadmin 账号不能被任何人禁用/删除
        if (rolePolicy.isProtectedRootUsername(targetUsername) &&
            ("禁用".equals(action) || "删除".equals(action))) {
            throw new BizException(BizCode.FORBIDDEN, "superadmin 账号不能被" + action);
        }
        
        // 不能操作优先级 >= 自己的用户
        if (!rolePolicy.canManageRole(operatorRoleCode, targetRoleCode)) {
            throw new BizException(BizCode.FORBIDDEN, 
                "没有权限" + action + "该用户（目标角色: " + targetRoleCode + "）");
        }
    }

    /**
     * 获取用户的角色代码
     */
    private String getUserRoleCode(SysUser user) {
        if (user.getRoleId() == null) return null;
        SysRole role = roleMapper.selectById(user.getRoleId());
        return role != null ? role.getRoleCode() : null;
    }

    @Override
    public PageResponse<UserResponse> listUsers(UserListRequest request, String operatorRoleCode) {
        // 构建查询条件
        Page<SysUser> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(request.getUsername())) {
            wrapper.like(SysUser::getUsername, request.getUsername());
        }
        if (request.getRoleId() != null) {
            wrapper.eq(SysUser::getRoleId, request.getRoleId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, request.getStatus());
        }
        
        // 根据操作者角色过滤可见用户
        // SUPER_ADMIN 可以看到所有用户
        // ADMIN 只能看到非 SUPER_ADMIN 和非 ADMIN 角色的用户
        if (!rolePolicy.isSuperAdmin(operatorRoleCode)) {
            // 查询 SUPER_ADMIN 和 ADMIN 角色的 ID，排除这些角色的用户
            LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.in(SysRole::getRoleCode, rolePolicy.managementRoleCodes());
            List<SysRole> excludedRoles = roleMapper.selectList(roleWrapper);
            if (!excludedRoles.isEmpty()) {
                List<Long> excludedRoleIds = excludedRoles.stream()
                    .map(SysRole::getId)
                    .collect(Collectors.toList());
                wrapper.notIn(SysUser::getRoleId, excludedRoleIds);
            }
        }
        
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        // 执行分页查询
        Page<SysUser> result = userMapper.selectPage(page, wrapper);
        Map<Long, SysRole> roleMap = buildRoleMap(result.getRecords());
        
        // 转换为响应对象（需要关联角色信息）
        List<UserResponse> userList = result.getRecords().stream()
            .map(user -> convertToResponse(user, roleMap.get(user.getRoleId())))
            .collect(Collectors.toList());
        
        return PageResponse.of(userList, result.getTotal(), request.getPage(), request.getSize());
    }

    @Override
    public UserResponse getUserById(Long id) {
        SysUser user = userMapper.selectUserWithRoleById(id);
        if (user == null) {
            throw new BizException(BizCode.NOT_FOUND, "用户不存在");
        }
        return convertToResponseWithRole(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request, String operatorRoleCode) {
        validateStatus(request.getStatus());

        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.CONFLICT, "用户名已存在");
        }
        
        // 检查角色是否存在
        SysRole role = roleMapper.selectById(request.getRoleId());
        if (role == null) {
            throw new BizException(BizCode.BAD_REQUEST, "指定的角色不存在");
        }
        
        // 权限检查：不能创建优先级 >= 自己的角色用户
        if (!rolePolicy.canManageRole(operatorRoleCode, role.getRoleCode())) {
            throw new BizException(BizCode.FORBIDDEN, 
                "没有权限创建该角色的用户（目标角色: " + role.getRoleCode() + "）");
        }
        
        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoleId(request.getRoleId());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        
        userMapper.insert(user);
        log.info("创建用户成功: username={}, roleId={}", user.getUsername(), user.getRoleId());
        
        // 返回完整信息
        return getUserById(user.getId());
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, String operatorRoleCode) {
        validateStatus(request.getStatus());

        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(BizCode.NOT_FOUND, "用户不存在");
        }
        
        // 权限检查
        String targetRoleCode = getUserRoleCode(user);
        checkOperationPermission(operatorRoleCode, targetRoleCode, user.getUsername(), "修改");
        
        // 更新用户名
        if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
            // 检查新用户名是否已被使用
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, request.getUsername());
            wrapper.ne(SysUser::getId, id);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BizException(BizCode.CONFLICT, "用户名已存在");
            }
            user.setUsername(request.getUsername());
        }
        
        // 更新密码
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            // 密码变更，递增 tokenVersion 使旧 Token 失效
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        
        // 更新角色
        if (request.getRoleId() != null && !request.getRoleId().equals(user.getRoleId())) {
            SysRole role = roleMapper.selectById(request.getRoleId());
            if (role == null) {
                throw new BizException(BizCode.BAD_REQUEST, "指定的角色不存在");
            }
            // 检查目标角色优先级
            if (!rolePolicy.canManageRole(operatorRoleCode, role.getRoleCode())) {
                throw new BizException(BizCode.FORBIDDEN, 
                    "没有权限将用户角色设置为: " + role.getRoleCode());
            }
            user.setRoleId(request.getRoleId());
            // 角色变更，递增 tokenVersion 使旧 Token 失效
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        
        // 更新状态
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        
        userMapper.updateById(user);
        log.info("更新用户成功: id={}, username={}", id, user.getUsername());
        
        return getUserById(id);
    }

    @Override
    @Transactional
    public UserResponse changeUserRole(Long id, Long roleId, String operatorRoleCode) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(BizCode.NOT_FOUND, "用户不存在");
        }
        
        // 权限检查：检查是否有权限操作目标用户
        String targetRoleCode = getUserRoleCode(user);
        checkOperationPermission(operatorRoleCode, targetRoleCode, user.getUsername(), "修改角色");
        
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(BizCode.BAD_REQUEST, "指定的角色不存在");
        }
        
        // 权限检查：检查是否有权限设置为目标角色
        if (!rolePolicy.canManageRole(operatorRoleCode, role.getRoleCode())) {
            throw new BizException(BizCode.FORBIDDEN, 
                "没有权限将用户角色设置为: " + role.getRoleCode());
        }
        
        // 只有角色真正变更时才更新 tokenVersion
        if (!roleId.equals(user.getRoleId())) {
            user.setRoleId(roleId);
            user.setTokenVersion(user.getTokenVersion() + 1);
            userMapper.updateById(user);
            log.info("修改用户角色成功: id={}, newRoleId={}, tokenVersion={}", 
                    id, roleId, user.getTokenVersion());
        }
        
        return getUserById(id);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(Long id, Integer status, String operatorRoleCode) {
        validateRequiredStatus(status);

        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(BizCode.NOT_FOUND, "用户不存在");
        }
        
        // 权限检查
        String targetRoleCode = getUserRoleCode(user);
        checkOperationPermission(operatorRoleCode, targetRoleCode, user.getUsername(), 
            status == 0 ? "禁用" : "启用");

        user.setStatus(status);
        userMapper.updateById(user);
        log.info("修改用户状态成功: id={}, status={}", id, status);
        
        return getUserById(id);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, String operatorRoleCode) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(BizCode.NOT_FOUND, "用户不存在");
        }
        
        // 权限检查
        String targetRoleCode = getUserRoleCode(user);
        checkOperationPermission(operatorRoleCode, targetRoleCode, user.getUsername(), "删除");
        
        userMapper.deleteById(id);
        log.info("删除用户成功: id={}, username={}", id, user.getUsername());
    }

    @Override
    @Transactional
    public int batchDelete(List<Long> ids, String operatorRoleCode) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        // 查询所有待删除用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getId, ids);
        List<SysUser> users = userMapper.selectList(wrapper);
        
        // 过滤：只保留有权限删除的用户
        List<Long> deleteIds = users.stream()
            .filter(user -> {
                // superadmin 账号不能被任何人删除
                if (rolePolicy.isProtectedRootUsername(user.getUsername())) {
                    return false;
                }
                // 只能删除比自己权限低的用户
                String targetRoleCode = getUserRoleCode(user);
                return rolePolicy.canManageRole(operatorRoleCode, targetRoleCode);
            })
            .map(SysUser::getId)
            .collect(Collectors.toList());
        
        if (deleteIds.isEmpty()) {
            throw new BizException(BizCode.FORBIDDEN, "没有权限删除选中的用户");
        }
        
        int deleted = userMapper.deleteBatchIds(deleteIds);
        log.info("批量删除用户成功: count={}, ids={}", deleted, deleteIds);
        
        // 如果有用户被跳过，记录日志
        if (deleteIds.size() < ids.size()) {
            log.warn("批量删除时跳过了 {} 个无权限操作的用户", ids.size() - deleteIds.size());
        }
        
        return deleted;
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword, String operatorRoleCode) {
        SysUser targetUser = userMapper.selectById(id);
        if (targetUser == null) {
            throw new BizException(BizCode.NOT_FOUND, "用户不存在");
        }

        // 查询目标用户的角色
        SysRole targetRole = roleMapper.selectById(targetUser.getRoleId());
        String targetRoleCode = targetRole != null ? targetRole.getRoleCode() : null;

        // 权限检查：
        // 1. SUPER_ADMIN 可以重置任何人的密码
        // 2. ADMIN 只能重置非管理员(SUPER_ADMIN/ADMIN)用户的密码
        if (!rolePolicy.canResetCredential(operatorRoleCode, targetRoleCode)) {
            throw new BizException(BizCode.FORBIDDEN, "没有权限重置该用户的密码");
        }

        // 更新密码并递增 tokenVersion
        targetUser.setPassword(passwordEncoder.encode(newPassword));
        targetUser.setTokenVersion(targetUser.getTokenVersion() + 1);
        userMapper.updateById(targetUser);
        log.info("重置用户凭证成功: id={}, operatorRole={}, tokenVersion={}", 
                id, operatorRoleCode, targetUser.getTokenVersion());
    }

    private Map<Long, SysRole> buildRoleMap(List<SysUser> users) {
        List<Long> roleIds = users.stream()
            .map(SysUser::getRoleId)
            .filter(roleId -> roleId != null)
            .distinct()
            .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
            .collect(Collectors.toMap(SysRole::getId, role -> role));
    }

    /**
     * 转换为响应对象（角色信息由批量查询结果提供）
     */
    private UserResponse convertToResponse(SysUser user, SysRole role) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRoleId(user.getRoleId());
        response.setStatus(user.getStatus());
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        
        // 设置角色信息
        if (role != null) {
            response.setRoleCode(role.getRoleCode());
            response.setRoleName(role.getRoleName());
        }
        
        return response;
    }

    /**
     * 转换为响应对象（已含角色信息）
     */
    private UserResponse convertToResponseWithRole(SysUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRoleId(user.getRoleId());
        response.setStatus(user.getStatus());
        response.setCreateTime(user.getCreateTime());
        response.setUpdateTime(user.getUpdateTime());
        
        if (user.getRole() != null) {
            response.setRoleCode(user.getRole().getRoleCode());
            response.setRoleName(user.getRole().getRoleName());
        } else if (user.getRoleCode() != null) {
            response.setRoleCode(user.getRoleCode());
        }
        
        return response;
    }

    private void validateRequiredStatus(Integer status) {
        if (status == null) {
            throw new BizException(BizCode.BAD_REQUEST, "状态值必须为0或1");
        }
        validateStatus(status);
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BizException(BizCode.BAD_REQUEST, "状态值必须为0或1");
        }
    }
}
