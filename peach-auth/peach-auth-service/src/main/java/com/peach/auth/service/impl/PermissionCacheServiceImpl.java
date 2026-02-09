//package com.peach.userservice.service.impl;
//
//import cn.dev33.satoken.stp.StpUtil;
//import com.peach.userservice.dao.RoleDao;
//import com.peach.userservice.entity.RoleDO;
//import com.peach.userservice.service.IPermissionCacheService;
//import com.peach.userservice.vo.RoleVO;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.util.List;
//
///**
// * 权限缓存服务实现类
// */
//@Slf4j
//@Service
//public class PermissionCacheServiceImpl implements IPermissionCacheService {
//
//    @Resource
//    private RoleDao roleDao;
//
//    @Resource
//    private StringRedisTemplate stringRedisTemplate;
//
//    // 与网关保持一致的 Redis Key 前缀
//    private static final String ROLE_PERMS_PREFIX = "PEACH:ROLE_PERMS:";
//
//    @Override
//    public void refreshRolePermissionCache(String roleCode) {
//        log.info("Refreshing permission cache for role: {}", roleCode);
//
//        // 1. 从数据库查询该角色的所有权限编码
//        List<String> permissions = roleDao.selectPermissionsByRoleCode(roleCode);
//
//        // 2. 存入 Redis 共享缓存 (逗号分隔字符串，永久有效)
//        String key = ROLE_PERMS_PREFIX + roleCode;
//        if (permissions != null && !permissions.isEmpty()) {
//            stringRedisTemplate.opsForValue().set(key, String.join(",", permissions));
//        } else {
//            stringRedisTemplate.delete(key);
//        }
//    }
//
//    @Override
//    public void refreshAllRolePermissions() {
//        log.info("Initializing all role-permission caches...");
//
//        // 1. 查询所有有效角色
//        RoleDO query = new RoleDO();
//        query.setIsDelete(0);
//        List<RoleVO> roles = roleDao.select(query);
//
//        // 2. 逐个刷新缓存
//        for (RoleVO role : roles) {
//            refreshRolePermissionCache(role.getRoleCode());
//        }
//        log.info("All role-permission caches initialized.");
//    }
//
//    @Override
//    public void clearUserPermissionCache(String userId) {
//        log.info("Clearing Sa-Token permission cache for user: {}", userId);
//        // 清除 Sa-Token 自动生成的二级缓存，强制下次触发 StpInterfaceImpl
//        StpUtil.deletePermissionCache(userId);
//    }
//}
