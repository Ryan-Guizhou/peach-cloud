package com.peach.satoken.quickstart.web;

import com.peach.satoken.quickstart.scenario.AuthScenarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 最小登录/登出/鉴权 Controller。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@RestController
public class AuthDemoController {

    private final AuthScenarioService scenarioService;

    /**
     * @param scenarioService 鉴权场景服务
     */
    public AuthDemoController(AuthScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    /**
     * 登录。
     *
     * @param request username/password
     * @return token 信息
     */
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        try {
            return scenarioService.login(request.get("username"), request.get("password"));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
    }

    /**
     * 登出。
     *
     * @return 固定结果
     */
    @PostMapping("/auth/logout")
    public Map<String, Object> logout() {
        scenarioService.logout();
        return Map.of("logout", true);
    }

    /**
     * 需要登录的受保护接口。
     *
     * @return 当前用户
     */
    @GetMapping("/api/profile")
    public Map<String, Object> profile() {
        return scenarioService.currentProfile();
    }
}
