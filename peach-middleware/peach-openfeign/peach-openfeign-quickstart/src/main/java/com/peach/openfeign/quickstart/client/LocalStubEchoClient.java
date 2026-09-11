package com.peach.openfeign.quickstart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 指向本机 Stub 下游的 Feign 客户端。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@FeignClient(name = "localStubEchoClient", url = "${quickstart.openfeign.downstream-url}")
public interface LocalStubEchoClient {

    /**
     * 调用本地 Stub echo。
     *
     * @param message 消息
     * @return 下游响应
     */
    @GetMapping("/stub/echo")
    Map<String, String> echo(@RequestParam("message") String message);
}
