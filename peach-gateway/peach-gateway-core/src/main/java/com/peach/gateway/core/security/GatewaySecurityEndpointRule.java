package com.peach.gateway.core.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 网关公开端点匹配规则。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GatewaySecurityEndpointRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 匹配所有 HTTP 方法的通配值。
     */
    public static final String ANY_METHOD = "ANY";

    /**
     * 需要匹配的 HTTP 方法，或 {@link #ANY_METHOD}。
     */
    private String method;

    /**
     * Ant 风格请求路径表达式。
     */
    private String path;

    /**
     * 创建匹配任意请求方法的端点规则。
     *
     * @param path Ant 风格请求路径表达式
     * @return 端点规则
     */
    public static GatewaySecurityEndpointRule any(String path) {
        return new GatewaySecurityEndpointRule(ANY_METHOD, path);
    }

    /**
     * 创建匹配指定请求方法和路径的端点规则。
     *
     * @param method HTTP 方法
     * @param path Ant 风格请求路径表达式
     * @return 端点规则
     */
    public static GatewaySecurityEndpointRule of(String method, String path) {
        return new GatewaySecurityEndpointRule(method, path);
    }
}
