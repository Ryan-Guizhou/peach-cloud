package com.peach.satoken.quickstart.example;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示 Sa-Token 登录、会话读取与登出。必须在 Web 请求上下文中调用。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:10
 */
@Slf4j
@Indexed
@Service
public class LoginExample {

    private final String demoUsername;
    private final String demoPassword;

    public LoginExample(@Value("${quickstart.satoken.demo.username}") String demoUsername,
                        @Value("${quickstart.satoken.demo.password}") String demoPassword) {
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    /**
     * 校验开发占位账号后登录，返回 token 名称与值。
     *
     * @param username 用户名
     * @param password 密码
     * @return token 信息（不含密码）
     */
    public Map<String, Object> login(String username, String password) {
        if (!demoUsername.equals(username) || !demoPassword.equals(password)) {
            throw new IllegalArgumentException("invalid credentials");
        }
        StpUtil.login(username);
        String loginId = StpUtil.getLoginIdAsString();
        String tokenName = StpUtil.getTokenName();
        String tokenValue = StpUtil.getTokenValue();
        log.info("login accepted, loginIdPresent={}, tokenPresent={}",
                loginId != null, tokenValue != null);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginId", loginId);
        body.put("tokenName", tokenName);
        body.put("tokenValue", tokenValue);
        return body;
    }

    /**
     * 登出当前会话。
     */
    public void logout() {
        StpUtil.logout();
        log.info("logout finished");
    }

    /**
     * 读取当前登录身份；未登录时由 {@code StpUtil.checkLogin()} 抛出。
     *
     * @return 当前用户信息
     */
    public Map<String, Object> currentProfile() {
        StpUtil.checkLogin();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginId", StpUtil.getLoginIdAsString());
        body.put("login", true);
        return body;
    }
}
