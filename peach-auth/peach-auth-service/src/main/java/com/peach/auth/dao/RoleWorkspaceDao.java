package com.peach.auth.dao;

import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.*;
import com.peach.auth.vo.*;
import com.peach.satoken.context.UserContext;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 角色工作区的限定作用域查询与原子写入；role携带服务端验证后的租户机构年度。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@MybatisDao
public interface RoleWorkspaceDao {
    /** 校验当前上下文的显式资源权限。 */
    int hasPermission(@Param("actor") UserContext actor, @Param("permission") String permission);
    /** 锁定角色，串行化当前工作区写操作。 */
    RoleVO lockRole(@Param("id") String id, @Param("actor") UserContext actor);
    List<String> functions(RoleDO role);
    List<AuthResourceVO> resources(RoleDO role);
    List<MenuVO> menus(RoleDO role);
    List<UserVO> users(RoleDO role);
    List<String> memberCodes(RoleDO role);
    void deleteFunctions(RoleDO role);
    void deleteResources(RoleDO role);
    void deleteMembers(RoleDO role);
    void insertFunction(AuthFunctionDO row);
    void insertResource(AuthResourceDO row);
    void insertMember(AuthPartyDO row);
    int countReferences(RoleDO role);
    void updateDefault(@Param("role") RoleDO role, @Param("url") String url);
}
