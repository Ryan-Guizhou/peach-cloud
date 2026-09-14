package com.peach.observability.quickstart.web;

import com.peach.observability.quickstart.example.RequestIdFilterExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 可观测性 REST 演示：回显 {@code RequestIdServletFilter} 写入的 requestId。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class ObservabilityDemoController {

    private final RequestIdFilterExample requestIdFilterExample;

    /**
     * 返回当前请求的 requestId，用于验证过滤器写入响应头与 MDC。
     *
     * @return ping 响应
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> body = requestIdFilterExample.ping();
        log.info("demo ping, requestIdPresent={}", body.get("requestId") != null && !body.get("requestId").isBlank());
        return body;
    }
}
