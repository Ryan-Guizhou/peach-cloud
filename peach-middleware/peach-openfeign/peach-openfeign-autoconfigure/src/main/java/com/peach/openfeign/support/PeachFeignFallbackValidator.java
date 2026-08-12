package com.peach.openfeign.support;

import com.peach.openfeign.config.PeachOpenfeignProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Feign 客户端降级（fallback/fallbackFactory）缺失校验器。
 *
 * <p>该组件在应用启动阶段（{@link InitializingBean#afterPropertiesSet()}）执行，
 * 用于扫描所有已注册的 Feign 客户端（通过 {@link FeignClientFactoryBean}），
 * 检查它们是否配置了 {@code fallback} 或 {@code fallbackFactory} 属性。</p>
 *
 * <p><b>目的：</b>提前发现缺少降级处理的 Feign 客户端，避免在运行时因下游服务故障
 * 导致调用失败且无降级逻辑而引发级联异常。特别在非生产环境（如开发、测试）中，
 * 若配置了快速失败模式，会直接抛出启动异常，强制开发者补齐降级策略。</p>
 *
 * <p><b>配置项：</b>通过 {@link PeachOpenfeignProperties.Fallback} 控制：
 * <ul>
 *   <li>{@code validateOnStartup}：是否启用启动时校验（默认 true）</li>
 *   <li>{@code failFastIfMissing}：缺失时是否快速失败（抛异常，默认 true）</li>
 *   <li>{@code productionProfiles}：生产环境 profile 列表（如 ["prod","prd"]），
 *       在此列表中即使缺失也不抛异常（仅 warn 日志），避免启动阻断</li>
 * </ul>
 * </p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/8/12
 */
@Slf4j
public class PeachFeignFallbackValidator implements InitializingBean {

    private final PeachOpenfeignProperties properties;
    private final Environment environment;
    private final Map<String, FeignClientFactoryBean> feignClients;

    /**
     * 构造器，注入配置、环境及所有 Feign 客户端工厂 Bean。
     *
     * @param properties   Peach OpenFeign 配置
     * @param environment  Spring 环境（用于识别当前激活的 profile）
     * @param feignClients 容器中所有 FeignClientFactoryBean 的 Map，key 为 Bean 名称
     */
    public PeachFeignFallbackValidator(PeachOpenfeignProperties properties,
                                       Environment environment,
                                       Map<String, FeignClientFactoryBean> feignClients) {
        this.properties = properties;
        this.environment = environment;
        this.feignClients = feignClients;
    }


    @Override
    public void afterPropertiesSet() {
        // 未启用启动校验或无 Feign 客户端则跳过
        if (!properties.getFallback().isValidateOnStartup() || feignClients == null || feignClients.isEmpty()) {
            log.debug("Feign fallback validation disabled or no Feign clients found, skip");
            return;
        }

        boolean production = isProductionProfile();
        log.debug("Starting Feign fallback validation, production profile: {}", production);

        int missingCount = 0;
        for (Map.Entry<String, FeignClientFactoryBean> entry : feignClients.entrySet()) {
            FeignClientFactoryBean factoryBean = entry.getValue();
            if (!hasFallback(factoryBean)) {
                missingCount++;
                String clientName = resolveClientName(factoryBean);
                String beanName = entry.getKey();
                String message = String.format(
                        "[PeachFeign] Feign client missing fallback/fallbackFactory, beanName='%s', client='%s'",
                        beanName, clientName
                );

                // 生产环境仅 warn，非生产环境按 fail-fast 配置处理
                if (!production && properties.getFallback().isFailFastIfMissing()) {
                    log.error("{} (fail-fast enabled)", message);
                    throw new IllegalStateException(message);
                }
                log.warn("{} (production={}, fail-fast={})", message, production,
                        properties.getFallback().isFailFastIfMissing());
            }
        }

        if (missingCount == 0) {
            log.info("Feign fallback validation passed, all {} clients have fallback configured",
                    feignClients.size());
        } else {
            log.info("Feign fallback validation completed, {} clients missing fallback, " +
                            "production={}, fail-fast={}", missingCount, production,
                    properties.getFallback().isFailFastIfMissing());
        }
    }

    /**
     * 判断当前激活的 Spring Profile 是否为生产环境。
     * <p>依次检查：</p>
     * <ol>
     *   <li>当前激活的 profile 是否在配置的 {@code productionProfiles} 列表中</li>
     *   <li>若未激活任何 profile，则检查默认 profile 是否包含 "prod"（兜底）</li>
     * </ol>
     *
     * @return 如果匹配生产环境 profile 则返回 {@code true}，否则 {@code false}
     */
    private boolean isProductionProfile() {
        Set<String> productionProfiles = new HashSet<>(properties.getFallback().getProductionProfiles());
        // 检查激活的 profile
        for (String activeProfile : environment.getActiveProfiles()) {
            if (productionProfiles.contains(activeProfile)) {
                return true;
            }
        }
        // 如果没有任何激活的 profile，检查默认 profile 是否包含 "prod"
        return Arrays.asList(environment.getDefaultProfiles()).contains("prod");
    }

    /**
     * 检查 Feign 客户端工厂是否配置了 fallback 或 fallbackFactory。
     *
     * @param factoryBean Feign 客户端工厂 Bean
     * @return 若任一属性配置了有效类型（非 Void）则返回 {@code true}
     */
    private boolean hasFallback(FeignClientFactoryBean factoryBean) {
        Class<?> fallback = readClass(factoryBean, "getFallback");
        Class<?> fallbackFactory = readClass(factoryBean, "getFallbackFactory");
        return isConfigured(fallback) || isConfigured(fallbackFactory);
    }

    /**
     * 解析 Feign 客户端的服务名称（用于日志展示）。
     * <p>优先使用 {@code name}，若为空则使用 {@code contextId}，否则返回 "unknown"。</p>
     *
     * @param factoryBean Feign 客户端工厂 Bean
     * @return 可读的服务名称
     */
    private String resolveClientName(FeignClientFactoryBean factoryBean) {
        Object name = invokeObject(factoryBean, "getName");
        if (name instanceof String && !((String) name).trim().isEmpty()) {
            return (String) name;
        }
        Object contextId = invokeObject(factoryBean, "getContextId");
        if (contextId instanceof String && !((String) contextId).trim().isEmpty()) {
            return (String) contextId;
        }
        return "unknown";
    }

    /**
     * 判断给定类型是否为有效配置（非 Void 类型）。
     *
     * @param clazz 类型
     * @return 若 clazz 不为 null 且不是 Void 类型则返回 {@code true}
     */
    private boolean isConfigured(Class<?> clazz) {
        return clazz != null && !Void.TYPE.equals(clazz) && !Void.class.equals(clazz);
    }

    /**
     * 通过反射调用 FeignClientFactoryBean 的指定 getter 方法，返回类型。
     *
     * @param factoryBean Feign 客户端工厂 Bean
     * @param methodName  方法名（如 "getFallback"）
     * @return 方法返回的 Class 对象，若异常则返回 null
     */
    private Class<?> readClass(FeignClientFactoryBean factoryBean, String methodName) {
        Object value = invokeObject(factoryBean, methodName);
        if (value instanceof Class) {
            return (Class<?>) value;
        }
        return null;
    }

    /**
     * 通用反射调用方法，用于获取 FeignClientFactoryBean 的某个属性。
     *
     * @param factoryBean Feign 客户端工厂 Bean
     * @param methodName  方法名
     * @return 方法返回值，若异常则返回 null
     */
    private Object invokeObject(FeignClientFactoryBean factoryBean, String methodName) {
        try {
            Method method = FeignClientFactoryBean.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(factoryBean);
        } catch (Exception ex) {
            log.debug("[PeachFeign] Failed to invoke method '{}' on FeignClientFactoryBean: {}",
                    methodName, ex.getMessage());
        }
        return null;
    }
}
