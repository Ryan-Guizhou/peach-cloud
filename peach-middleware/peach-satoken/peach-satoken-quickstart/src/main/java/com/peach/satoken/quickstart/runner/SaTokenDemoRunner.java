package com.peach.satoken.quickstart.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 启动后通过本机 HTTP 演示登录拿 token、鉴权通过、登出后拒绝。
 *
 * <p>必须走 Web 请求，避免直接调用 {@code StpUtil} 时 SaTokenContext 未初始化。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:10
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "quickstart.satoken.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenDemoRunner implements ApplicationRunner {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String username;
    private final String password;
    private final int port;

    public SaTokenDemoRunner(@Value("${quickstart.satoken.demo.username}") String username,
                             @Value("${quickstart.satoken.demo.password}") String password,
                             @Value("${server.port}") int port) {
        this.username = username;
        this.password = password;
        this.port = port;
    }

    @Override
    public void run(ApplicationArguments args) {
        runLoginDemo();
        runAuthorizedAccessDemo();
        runLogoutAndRejectDemo();
        log.info("satoken demo finished");
    }

    private void runLoginDemo() {
        log.info("=== Login demo ===");
        Map<String, Object> body = login();
        if (body.get("tokenValue") == null || body.get("loginId") == null) {
            throw new IllegalStateException("login demo self-check failed");
        }
        log.info("login demo ok, loginIdPresent=true, tokenPresent=true");
    }

    private void runAuthorizedAccessDemo() {
        log.info("=== Authorized access demo ===");
        TokenHolder token = TokenHolder.from(login());
        Map<String, Object> profile = getProfile(token, true);
        if (!Boolean.TRUE.equals(profile.get("login")) || profile.get("loginId") == null) {
            throw new IllegalStateException("authorized access demo self-check failed");
        }
        log.info("authorized access ok, loginIdPresent=true");
    }

    private void runLogoutAndRejectDemo() {
        log.info("=== Logout and reject demo ===");
        TokenHolder token = TokenHolder.from(login());
        logout(token);
        boolean rejected = false;
        try {
            getProfile(token, false);
        } catch (HttpClientErrorException.Unauthorized ex) {
            rejected = true;
        }
        if (!rejected) {
            throw new IllegalStateException("logout reject demo self-check failed");
        }
        log.info("logout reject ok, rejectedAsExpected=true");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("username", username, "password", password), headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/auth/login", request, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("login demo failed");
        }
        return response.getBody();
    }

    private void logout(TokenHolder token) {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/auth/logout", HttpMethod.POST, token.entity(), Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("logout demo failed");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProfile(TokenHolder token, boolean requireSuccess) {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/profile", HttpMethod.GET, token.entity(), Map.class);
        if (requireSuccess && (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null)) {
            throw new IllegalStateException("profile demo failed");
        }
        return response.getBody();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    private record TokenHolder(String tokenName, String tokenValue) {

        private static TokenHolder from(Map<String, Object> loginBody) {
            Object tokenName = loginBody.get("tokenName");
            Object tokenValue = loginBody.get("tokenValue");
            if (tokenName == null || tokenValue == null) {
                throw new IllegalStateException("login response missing token");
            }
            return new TokenHolder(String.valueOf(tokenName), String.valueOf(tokenValue));
        }

        private HttpEntity<Void> entity() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(tokenName, tokenValue);
            return new HttpEntity<>(headers);
        }
    }
}
