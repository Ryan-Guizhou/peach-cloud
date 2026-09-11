package com.peach.openfeign.quickstart.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地下游 Stub，供 Feign Client 闭环调用，不依赖外部服务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/stub")
public class DownstreamStubController {

    /**
     * 成功回显。
     *
     * @param message 回显内容
     * @return 固定 source=stub 的响应
     */
    @GetMapping("/echo")
    public Map<String, String> echo(@RequestParam("message") String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("source", "stub");
        log.info("stub echo, message={}", message);
        return body;
    }

    /**
     * 返回 500，触发 ErrorDecoder 的远端失败分类。
     */
    @GetMapping("/fail")
    public ResponseEntity<Void> fail() {
        log.info("stub fail, status=500");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 返回 408，触发 ErrorDecoder 的超时分类。
     */
    @GetMapping("/timeout")
    public ResponseEntity<Void> timeout() {
        log.info("stub timeout, status=408");
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
    }
}
