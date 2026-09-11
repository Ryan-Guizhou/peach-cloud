package com.peach.openfeign.quickstart.scenario;

import com.peach.openfeign.quickstart.client.LocalStubEchoClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * OpenFeign 本地 Stub 调用场景。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class OpenFeignScenarioService {

    private final LocalStubEchoClient localStubEchoClient;

    /**
     * @param localStubEchoClient 本地 Stub Feign 客户端
     */
    public OpenFeignScenarioService(LocalStubEchoClient localStubEchoClient) {
        this.localStubEchoClient = localStubEchoClient;
    }

    /**
     * 通过 Feign 调用本机 Stub。
     *
     * @param message 消息
     * @return 下游响应
     */
    public Map<String, String> echoViaFeign(String message) {
        return localStubEchoClient.echo(message);
    }
}
