package com.peach.scheduled.rest.internal;

import org.springframework.stereotype.Indexed;

import com.peach.satoken.config.UserContextProperties;
import com.peach.satoken.security.SatokenEndpointRule;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler 内部接口用户上下文白名单配置。
 *
 * <p>Claim 和 Handler 注册属于服务间调用，没有终端用户登录上下文，因此需要跳过
 * UserContextFilter 的用户登录恢复；接口本身仍由 SchedulerInternalController 显式校验 Same-Token。
 * 这里只解决无用户上下文的调用模型，不把内部接口降级为匿名接口。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Configuration(proxyBeanMethods = false)
@Indexed
public class SchedulerInternalSecurityConfiguration {

    /**
     * 创建 Scheduler 内部接口安全配置。
     */
    public SchedulerInternalSecurityConfiguration() {
        // Intentionally empty.
    }

    /**
     * 将 Scheduler 服务间内部端点加入用户上下文白名单。
     *
     * <p>白名单只跳过用户登录上下文恢复；Same-Token 由内部 Controller 再次显式校验。</p>
     *
     * @param properties 用户上下文配置
     * @return 启动完成后的白名单补充任务
     */
    @Bean
    public SmartInitializingSingleton schedulerInternalEndpointRules(UserContextProperties properties) {
        return () -> {
            List<SatokenEndpointRule> rules = new ArrayList<SatokenEndpointRule>();
            if (properties.getPublicEndpoints() != null) {
                rules.addAll(properties.getPublicEndpoints());
            }
            addRuleIfMissing(rules, "POST", "/internal/scheduler/executions/*/claim");
            addRuleIfMissing(rules, "POST", "/internal/scheduler/handlers/register");
            properties.setPublicEndpoints(rules);
        };
    }

    private void addRuleIfMissing(List<SatokenEndpointRule> rules, String method, String path) {
        for (SatokenEndpointRule rule : rules) {
            if (method.equalsIgnoreCase(rule.getMethod()) && path.equals(rule.getPath())) {
                return;
            }
        }
        rules.add(SatokenEndpointRule.of(method, path));
    }
}
