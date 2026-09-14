package com.peach.satoken.quickstart.web;

import cn.dev33.satoken.exception.NotLoginException;
import com.peach.satoken.quickstart.example.LoginExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 最小登录 / 鉴权 / 登出 HTTP 入口，保证 Sa-Token 运行在 Web 请求上下文中。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:10
 */
@Slf4j
@Indexed
@RestController
@RequiredArgsConstructor
public class AuthDemoController {

    private final LoginExample loginExample;

    /**
     * 登录并返回 token。
     *
     * @param request username/password
     * @return token 信息
     */
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        try {
            return loginExample.login(request.get("username"), request.get("password"));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
    }

    /**
     * 登出当前会话。
     *
     * @return 固定结果
     */
    @PostMapping("/auth/logout")
    public Map<String, Object> logout() {
        loginExample.logout();
        return Map.of("logout", true);
    }

    /**
     * 需要登录的受保护接口。
     *
     * @return 当前用户
     */
    @GetMapping("/api/profile")
    public Map<String, Object> profile() {
        try {
            return loginExample.currentProfile();
        } catch (NotLoginException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
    }
}
