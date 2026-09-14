package com.peach.openfeign.quickstart;

import com.peach.openfeign.exception.PeachFeignRemoteException;
import com.peach.openfeign.exception.PeachFeignTimeoutException;
import com.peach.openfeign.quickstart.example.LocalStubCallExample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证本机 Stub + Feign 闭环：成功调用，以及 ErrorDecoder 对 500/408 的分类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quickstart.openfeign.demo.enabled=false")
class OpenFeignCapabilityTest {

    /**
     * Feign 在 Bean 定义阶段就校验 URL，早于嵌入式容器分配 {@code local.server.port}，
     * 因此先预占随机端口再注入 {@code server.port} 与 downstream-url。
     */
    private static final int TEST_PORT = availablePort();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void registerDownstreamUrl(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> TEST_PORT);
        registry.add("quickstart.openfeign.downstream-url", () -> "http://127.0.0.1:" + TEST_PORT);
    }

    private static int availablePort() {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(0));
            return socket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("no free port for OpenFeign test", ex);
        }
    }

    @Autowired
    private LocalStubCallExample localStubCallExample;

    @Test
    void shouldEchoViaLocalStubFeignClient() {
        assertThat(port).isEqualTo(TEST_PORT);
        Map<String, String> body = localStubCallExample.echo("it-message");
        assertThat(body).isNotNull();
        assertThat(body.get("source")).isEqualTo("stub");
        assertThat(body.get("message")).isEqualTo("it-message");
    }

    @Test
    void shouldClassifyHttp500AsRemoteException() {
        PeachFeignRemoteException exception = localStubCallExample.failAsRemoteError();
        assertThat(exception.getStatus()).isEqualTo(500);
        assertThat(exception.getClientName()).isEqualTo("LocalStubClient");
    }

    @Test
    void shouldClassifyHttp408AsTimeoutException() {
        PeachFeignTimeoutException exception = localStubCallExample.timeoutAsClassified();
        assertThat(exception.getClientName()).isEqualTo("LocalStubClient");
        assertThat(exception.getMethodKey()).contains("timeout");
    }
}
