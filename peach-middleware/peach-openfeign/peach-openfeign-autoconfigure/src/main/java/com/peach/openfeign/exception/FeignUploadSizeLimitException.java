package com.peach.openfeign.exception;

/**
 * Feign上传Size限流异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class FeignUploadSizeLimitException extends RuntimeException {

    private final String requestUrl;

    private final long contentLength;

    private final long maxBytes;

    public FeignUploadSizeLimitException(String requestUrl, long contentLength, long maxBytes) {
        super("Feign upload size exceeds limit");
        this.requestUrl = requestUrl;
        this.contentLength = contentLength;
        this.maxBytes = maxBytes;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getMaxBytes() {
        return maxBytes;
    }
}
