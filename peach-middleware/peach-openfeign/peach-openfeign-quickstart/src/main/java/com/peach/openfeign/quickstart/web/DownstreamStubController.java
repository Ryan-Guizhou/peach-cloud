package com.peach.openfeign.quickstart.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地下游 Stub，供 Feign Client 调用。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@RestController
@RequestMapping("/stub")
public class DownstreamStubController {

    /**
     * Echo 下游接口，可选回显 RequestId。
     *
     * @param message   消息
     * @param requestId 治理头
     * @return 响应
     */
    @GetMapping("/echo")
    public Map<String, String> echo(@RequestParam("message") String message,
                                    @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("requestId", requestId == null ? "" : requestId);
        body.put("source", "stub");
        return body;
    }
}
