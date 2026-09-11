package com.peach.openfeign.quickstart;

import com.peach.openfeign.exception.PeachFeignRemoteException;
import com.peach.openfeign.exception.PeachFeignTimeoutException;
import com.peach.openfeign.quickstart.example.LocalStubCallExample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证本机 Stub + Feign 闭环：成功调用，以及 ErrorDecoder 对 500/408 的分类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=18085",
                "quickstart.openfeign.downstream-url=http://127.0.0.1:18085",
                "quickstart.openfeign.demo.enabled=false"
        })
class OpenFeignCapabilityTest {

    @Autowired
    private LocalStubCallExample localStubCallExample;

    @Test
    void shouldEchoViaLocalStubFeignClient() {
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
