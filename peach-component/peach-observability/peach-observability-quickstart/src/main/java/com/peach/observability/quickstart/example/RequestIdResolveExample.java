package com.peach.observability.quickstart.example;

import com.peach.observability.core.RequestIdResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 注入自动装配的 {@link RequestIdResolver}：信任合法上游 ID，非法或空值时生成新 ID。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class RequestIdResolveExample {

    private final RequestIdResolver requestIdResolver;

    /**
     * 解析上游请求 ID；未信任或格式不合法时生成新值。
     *
     * @param incomingRequestId 上游请求头值，可以为空
     * @return 可安全写入 Header / MDC 的请求 ID
     */
    public String resolve(String incomingRequestId) {
        String resolved = requestIdResolver.resolve(incomingRequestId);
        boolean reused = incomingRequestId != null && incomingRequestId.trim().equals(resolved);
        log.info("requestId resolved, reusedIncoming={}, resultLength={}", reused, resolved.length());
        return resolved;
    }

    /**
     * 判断请求 ID 是否符合安全格式。
     *
     * @param requestId 待检查值
     * @return 合法时返回 {@code true}
     */
    public boolean isValid(String requestId) {
        return requestIdResolver.isValid(requestId);
    }
}
