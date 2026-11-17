package com.peach.auth.service.support;

/**
 * 全局永久业务标识的编码类型。
 *
 * <p>所有类型均在 PLATFORM 单一命名空间中分配，历史编号永不回收。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 14:50
 */
public enum AuthBusinessIdType {
    USER,
    TENANT,
    ORG,
    ROLE,
    MEMBER,
    INVITATION,
    FUNC,
    MENU,
    RESOURCE,
    ROUTER,
    AVATAR,
    AUTHRES,
    AUTHLOG,
    APP
}
