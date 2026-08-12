package com.peach.satoken.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 业务服务侧 Sa-Token 公开端点匹配规则。
 *
 * <p>该规则用于 Servlet 业务服务中的 Same-Token 拦截器和用户上下文过滤器，
 * 只表达 HTTP 方法与 Ant 风格路径，不承载权限、角色或风控语义。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SatokenEndpointRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 匹配所有 HTTP 方法的通配值。
     */
    public static final String ANY_METHOD = "ANY";

    /**
     * HTTP 方法；使用 {@link #ANY_METHOD} 表示不限制方法。
     */
    private String method;

    /**
     * Ant 风格请求路径表达式。
     */
    private String path;

    /**
     * 创建匹配任意 HTTP 方法的公开端点规则。
     *
     * @param path Ant 风格请求路径表达式
     * @return 公开端点规则
     */
    public static SatokenEndpointRule any(String path) {
        return new SatokenEndpointRule(ANY_METHOD, path);
    }

    /**
     * 创建匹配指定 HTTP 方法和路径的公开端点规则。
     *
     * @param method HTTP 方法
     * @param path Ant 风格请求路径表达式
     * @return 公开端点规则
     */
    public static SatokenEndpointRule of(String method, String path) {
        return new SatokenEndpointRule(method, path);
    }
}
