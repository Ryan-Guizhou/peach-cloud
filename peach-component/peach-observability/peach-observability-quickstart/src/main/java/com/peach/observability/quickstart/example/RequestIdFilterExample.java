package com.peach.observability.quickstart.example;

import com.peach.observability.core.ObservabilityConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 读取 {@link com.peach.observability.web.RequestIdServletFilter} 写入 MDC 的 requestId，供 REST 演示回显。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Service
public class RequestIdFilterExample {

    /**
     * 组装 ping 响应，包含当前 MDC 中的 requestId。
     *
     * @return 状态与 requestId
     */
    public Map<String, String> ping() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        String requestId = MDC.get(ObservabilityConstants.REQUEST_ID_MDC_KEY);
        body.put("requestId", requestId == null ? "" : requestId);
        log.info("filter ping, requestIdPresent={}", requestId != null && !requestId.isBlank());
        return body;
    }
}
