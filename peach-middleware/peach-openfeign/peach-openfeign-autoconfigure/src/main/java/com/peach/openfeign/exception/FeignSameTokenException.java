package com.peach.openfeign.exception;

/**
 * Same-Token 缺失且 fail-fast 开启时抛出。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class FeignSameTokenException extends RuntimeException {

    private final String requestUrl;

    public FeignSameTokenException(String requestUrl) {
        super("Same-Token is required but not available for Feign request");
        this.requestUrl = requestUrl;
    }

    public String getRequestUrl() {
        return requestUrl;
    }
}
