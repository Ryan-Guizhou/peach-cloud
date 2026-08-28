package com.peach.openfeign.exception;

/**
 * FeignSame令牌异常。
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
