package com.peach.openfeign.quickstart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 指向本机 Stub 的 Feign 客户端，与 {@code DownstreamStubController} 闭环。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@FeignClient(name = "localStubClient", url = "${quickstart.openfeign.downstream-url}")
public interface LocalStubClient {

    /**
     * 调用本机 Stub 成功回显。
     *
     * @param message 回显内容
     * @return Stub 响应
     */
    @GetMapping("/stub/echo")
    Map<String, String> echo(@RequestParam("message") String message);

    /**
     * 调用本机 Stub 的 500 错误接口，由 ErrorDecoder 分类为远端失败。
     */
    @GetMapping("/stub/fail")
    Map<String, String> fail();

    /**
     * 调用本机 Stub 的 408 接口，由 ErrorDecoder 分类为超时。
     */
    @GetMapping("/stub/timeout")
    Map<String, String> timeout();
}
