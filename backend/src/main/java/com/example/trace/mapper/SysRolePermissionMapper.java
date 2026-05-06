package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.SysRolePermission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色权限关联 Mapper
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 角色权限数量聚合结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class RolePermissionCount {
        private Long roleId;
        private Integer permissionCount;
    }

    /**
     * Delete all permissions for a role
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 按角色ID分组统计权限数量
     */
    @Select("""
        <script>
        SELECT role_id AS roleId, COUNT(permission_id) AS permissionCount
        FROM sys_role_permission
        WHERE role_id IN
        <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
            #{roleId}
        </foreach>
        GROUP BY role_id
        </script>
        """)
    List<RolePermissionCount> countPermissionByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 批量插入角色权限关联
     */
    @Insert("""
        <script>
        INSERT INTO sys_role_permission (role_id, permission_id) VALUES
        <foreach collection="permissionIds" item="permId" separator=",">
            (#{roleId}, #{permId})
        </foreach>
        </script>
        """)
    int batchInsert(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}
