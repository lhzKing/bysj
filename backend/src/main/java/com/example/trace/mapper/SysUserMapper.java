package com.example.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.trace.entity.SysUser;
import org.apache.ibatis.annotations.*;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    String USER_WITH_ROLE_BASE_SELECT = """
        SELECT u.*, r.role_code as roleCode, r.role_name as roleName
        FROM sys_user u
        LEFT JOIN sys_role r ON u.role_id = r.id
        """;

    /**
     * 根据用户名查询用户（关联角色信息）
     */
    @Select(USER_WITH_ROLE_BASE_SELECT + """
        WHERE u.username = #{username}
        LIMIT 1
        """)
    @Results(id = "sysUserWithRoleResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "roleId", column = "role_id"),
        @Result(property = "status", column = "status"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "roleCode", column = "roleCode"),
        @Result(property = "role.id", column = "role_id"),
        @Result(property = "role.roleCode", column = "roleCode"),
        @Result(property = "role.roleName", column = "roleName")
    })
    SysUser selectUserWithRole(@Param("username") String username);

    /**
     * 根据ID查询用户（关联角色信息）
     */
    @Select(USER_WITH_ROLE_BASE_SELECT + """
        WHERE u.id = #{id}
        """)
    @ResultMap("sysUserWithRoleResultMap")
    SysUser selectUserWithRoleById(@Param("id") Long id);
}
