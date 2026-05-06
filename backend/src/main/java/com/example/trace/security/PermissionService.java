package com.example.trace.security;

import com.example.trace.entity.SysPermission;
import com.example.trace.mapper.SysPermissionMapper;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.security.permission.ApiPermissionMatcher;
import com.example.trace.security.permission.PermissionCache;
import com.example.trace.security.permission.PermissionInheritanceResolver;
import com.example.trace.security.permission.RolePermissionQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Permission facade service responsible for permission lookup and checks.
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final RolePermissionQueryService queryService;
    private final PermissionInheritanceResolver inheritanceResolver;
    private final ApiPermissionMatcher apiPermissionMatcher;
    private final PermissionCache permissionCache;

    @Autowired
    public PermissionService(
            RolePermissionQueryService queryService,
            PermissionInheritanceResolver inheritanceResolver,
            ApiPermissionMatcher apiPermissionMatcher,
            PermissionCache permissionCache
    ) {
        this.queryService = queryService;
        this.inheritanceResolver = inheritanceResolver;
        this.apiPermissionMatcher = apiPermissionMatcher;
        this.permissionCache = permissionCache;
    }

    PermissionService(SysPermissionMapper permissionMapper, SysRoleMapper roleMapper) {
        this(
            new RolePermissionQueryService(permissionMapper, roleMapper),
            new PermissionInheritanceResolver(),
            new ApiPermissionMatcher(),
            new PermissionCache()
        );
    }

    public Set<String> getPermissionCodes(Long roleId) {
        return permissionCache.getPermissionCodes(roleId, () -> loadExpandedPermissionCodes(roleId));
    }

    public List<SysPermission> getApiPermissions(Long roleId) {
        return permissionCache.getApiPermissions(roleId, () -> loadApiPermissions(roleId));
    }

    public boolean hasPermission(Long roleId, String permCode) {
        if (permCode == null || permCode.isEmpty()) {
            return true;
        }
        Set<String> userPerms = getPermissionCodes(roleId);
        return userPerms.contains(permCode);
    }

    public boolean hasPermission(Long roleId, String[] permCodes, boolean matchAll) {
        if (permCodes == null || permCodes.length == 0) {
            return true;
        }

        Set<String> userPerms = getPermissionCodes(roleId);
        if (matchAll) {
            for (String code : permCodes) {
                if (!userPerms.contains(code)) {
                    return false;
                }
            }
            return true;
        }

        for (String code : permCodes) {
            if (userPerms.contains(code)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasApiPermission(Long roleId, String method, String path) {
        List<SysPermission> permissions = getApiPermissions(roleId);

        for (SysPermission permission : permissions) {
            if (apiPermissionMatcher.matches(permission, method, path)) {
                log.debug("Permission granted: {} {} matches {}:{}",
                    method, path, permission.getApiMethod(), permission.getApiPattern());
                return true;
            }
        }

        log.debug("Permission denied: {} {} for roleId={}", method, path, roleId);
        return false;
    }

    public Long getRoleIdByCode(String roleCode) {
        return queryService.getRoleIdByCode(roleCode);
    }

    public void clearCache() {
        // TODO 多实例部署需通过 Redis pub/sub 广播失效（参考 项目审查整改任务表_20260503.md T-P1-02 中期方案）。
        // 当前 PermissionCache 是 JVM 本地 ConcurrentHashMap，clear() 只清当前 JVM；
        // 其他实例的副本要等到该实例自身 clearCache 调用或重启才会失效。
        permissionCache.clear();
        log.info("Permission cache cleared");
    }

    public void clearCache(Long roleId) {
        // TODO 同 clearCache()：多实例下需广播失效消息，否则其它实例上 roleId 的缓存仍是旧值。
        permissionCache.clear(roleId);
        log.info("Permission cache cleared for roleId={}", roleId);
    }

    private Set<String> loadExpandedPermissionCodes(Long roleId) {
        List<SysPermission> permissions = queryService.getPermissionsByRoleId(roleId);
        Set<String> directCodes = new HashSet<>();
        for (SysPermission permission : permissions) {
            directCodes.add(permission.getPermCode());
        }

        Set<String> expandedCodes = inheritanceResolver.expand(directCodes);
        log.debug("Loaded {} permissions (including {} inherited) for roleId={}",
            expandedCodes.size(), expandedCodes.size() - directCodes.size(), roleId);
        return expandedCodes;
    }

    private List<SysPermission> loadApiPermissions(Long roleId) {
        List<SysPermission> permissions = queryService.getPermissionsByRoleId(roleId);
        log.debug("Loaded {} API permissions for roleId={}", permissions.size(), roleId);
        return permissions;
    }
}
