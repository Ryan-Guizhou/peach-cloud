//package com.peach.auth.service;
//
//import java.util.List;
//
///**
// * 权限缓存服务接口
// * 负责维护 Redis 中的角色-权限映射缓存
// */
//public interface IPermissionCacheService {
//
//    /**
//     * 刷新指定角色的权限缓存
//     * @param roleCode 角色编码
//     */
//    void refreshRolePermissionCache(String roleCode);
//
//    /**
//     * 刷新所有角色的权限缓存（系统初始化时使用）
//     */
//    void refreshAllRolePermissions();
//
//    /**
//     * 清除用户的权限缓存（强制下一次鉴权时从共享缓存重新加载）
//     * @param userId 用户ID
//     */
//    void clearUserPermissionCache(String userId);
//}
