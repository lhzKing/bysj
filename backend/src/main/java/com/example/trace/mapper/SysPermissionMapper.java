package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据角色ID查询权限列表
     */
    @Select("""
        SELECT p.* FROM sys_permission p
        INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        WHERE rp.role_id = #{roleId}
        """)
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色代码查询权限列表
     */
    @Select("""
        SELECT p.* FROM sys_permission p
        INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        INNER JOIN sys_role r ON r.id = rp.role_id
        WHERE r.role_code = #{roleCode}
        """)
    List<SysPermission> selectByRoleCode(@Param("roleCode") String roleCode);
}
