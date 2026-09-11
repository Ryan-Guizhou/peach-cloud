package com.peach.virtualthread.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证虚拟线程分组聚合场景与 HTTP 入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quickstart.virtual-thread.demo.enabled=false")
class VirtualThreadScenarioTest {

    @Autowired
    private VirtualThreadScenarioService scenarioService;

    @LocalServerPort
    private int port;

    @Test
    void shouldAggregateGroupedResults() throws Exception {
        String result = scenarioService.aggregate().get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("database-result:storage-result:remote-result");
    }

    @Test
    void shouldExposeAggregateEndpoint() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/demo/aggregate"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("database-result");
    }
}
