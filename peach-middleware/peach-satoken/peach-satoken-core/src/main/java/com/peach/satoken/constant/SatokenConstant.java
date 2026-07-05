package com.peach.satoken.constant;

/**
 * Sa-Token 相关常量。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public interface SatokenConstant {

    /**
     * 带用户信息的 token 拼接模板。
     */
    String TOKEN_INCLUDE_USER = "{0}-{1}-{2}";

    /**
     * 不带用户信息的 token 拼接模板。
     */
    String TOKEN_NOT_INCLUDE_USER = "{0}-{1}";

}
