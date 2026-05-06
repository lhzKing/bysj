package com.example.trace.security.permission;

import com.example.trace.entity.SysPermission;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Local in-memory cache for role permission lookups.
 *
 * <p><b>单实例假设（重要）</b>：本缓存使用 JVM 本地 {@link ConcurrentHashMap}，
 * {@link #clear()} / {@link #clear(Long)} 只清当前 JVM 的副本。多实例（水平扩展 / Pod 多副本）
 * 部署下，A 实例上由管理员触发的角色/权限变更不会立即同步到 B、C 实例的缓存——B、C 实例上
 * 已登录用户最长会持有 <b>到该实例缓存被显式 invalidate 或重启为止</b> 的过时权限视图。</p>
 *
 * <p>因此当前架构 <b>仅支持单实例部署</b>。多实例部署前必须先做改造（参考 {@code 项目审查整改任务表_20260503.md} T-P1-02 中期方案）：
 * 把缓存底层切到 Redis Hash + 短 TTL，并在角色/权限变更点 publish 失效消息让其他实例本地副本一并清空。</p>
 *
 * <p>另见：{@link com.example.trace.security.PermissionService#clearCache()} 上的 TODO。</p>
 */
@Component
public class PermissionCache {

    private final Map<Long, Set<String>> permissionCodeCache = new ConcurrentHashMap<>();
    private final Map<Long, List<SysPermission>> apiPermissionCache = new ConcurrentHashMap<>();

    public Set<String> getPermissionCodes(Long roleId, Supplier<Set<String>> loader) {
        return permissionCodeCache.computeIfAbsent(roleId, ignoredRoleId -> {
            Set<String> loadedCodes = loader.get();
            if (loadedCodes == null || loadedCodes.isEmpty()) {
                return Set.of();
            }
            return Set.copyOf(new LinkedHashSet<>(loadedCodes));
        });
    }

    public List<SysPermission> getApiPermissions(Long roleId, Supplier<List<SysPermission>> loader) {
        return apiPermissionCache.computeIfAbsent(roleId, ignoredRoleId -> {
            List<SysPermission> loadedPermissions = loader.get();
            if (loadedPermissions == null || loadedPermissions.isEmpty()) {
                return List.of();
            }
            return List.copyOf(loadedPermissions);
        });
    }

    public void clear() {
        permissionCodeCache.clear();
        apiPermissionCache.clear();
    }

    public void clear(Long roleId) {
        permissionCodeCache.remove(roleId);
        apiPermissionCache.remove(roleId);
    }
}
