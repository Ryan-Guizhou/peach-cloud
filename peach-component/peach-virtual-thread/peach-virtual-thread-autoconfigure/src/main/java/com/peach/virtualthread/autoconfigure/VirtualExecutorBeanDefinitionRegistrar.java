package com.peach.virtualthread.autoconfigure;

import com.peach.virtualthread.annotation.VirtualGroup;
import com.peach.virtualthread.api.VirtualExecutorService;
import com.peach.virtualthread.config.VirtualThreadProperties;
import com.peach.virtualthread.exception.DefaultVirtualTaskExceptionHandler;
import com.peach.virtualthread.exception.VirtualTaskExceptionHandler;
import com.peach.virtualthread.executor.DefaultVirtualExecutorService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 根据启动配置为每个静态业务组注册一个独立 {@code VirtualExecutorService} Bean。
 *
 * <p>遵循 Spring Boot 3 规范，采用函数式 {@code instanceSupplier} 进行编程式 Bean 注册与依赖自动注入，
 * 完全废弃 XML 时代的 {@code AUTOWIRE_CONSTRUCTOR} 模式，保证对 Spring Boot 3 Native 与 AOT 的原生兼容。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:20
 */
public final class VirtualExecutorBeanDefinitionRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    public static final String BEAN_SUFFIX = "VirtualExecutor";
    private static final Pattern GROUP_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");

    private Environment environment;
    private BeanFactory beanFactory;

    /**
     * 接收 Spring Environment，用于启动期绑定虚拟线程配置。
     *
     * @param environment 当前应用 Environment
     */
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 接收 Spring BeanFactory，用于在 instanceSupplier 中按需解析依赖。
     *
     * @param beanFactory 当前应用 BeanFactory
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * 根据启动配置注册所有静态业务组执行器 Bean。
     *
     * @param importingClassMetadata 导入自动配置类的元数据
     * @param registry 当前 BeanDefinitionRegistry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        VirtualThreadProperties properties = Binder.get(environment)
                .bind("peach.virtual-thread", Bindable.of(VirtualThreadProperties.class))
                .orElseGet(VirtualThreadProperties::new);
        if (!properties.isEnabled()) {
            return;
        }
        properties.validateForStartup();
        for (Map.Entry<String, VirtualThreadProperties.Group> entry : properties.getGroups().entrySet()) {
            registerGroup(registry, properties, entry.getKey(), entry.getValue());
        }
    }

    private void registerGroup(BeanDefinitionRegistry registry,
                               VirtualThreadProperties properties,
                               String groupName,
                               VirtualThreadProperties.Group group) {
        if (!GROUP_NAME.matcher(groupName).matches()) {
            throw new IllegalStateException("Invalid virtual thread group name: " + groupName);
        }
        String beanName = beanName(groupName);
        if (registry.containsBeanDefinition(beanName)) {
            throw new IllegalStateException("Duplicate virtual thread executor bean: " + beanName);
        }

        RootBeanDefinition definition = new RootBeanDefinition(DefaultVirtualExecutorService.class);
        definition.setTargetType(VirtualExecutorService.class);
        definition.addQualifier(new AutowireCandidateQualifier(VirtualGroup.class, groupName));
        // 关闭动作统一交给 VirtualExecutorRegistry，避免 Spring 对每个 Executor 重复推断 close()。
        definition.setDestroyMethodName("");
        // Spring Boot 3 推荐的函数式实例提供器，按类型自动解析未承接异常处理器，遗弃 XML 时代的 AUTOWIRE_CONSTRUCTOR。
        definition.setInstanceSupplier(() -> {
            VirtualTaskExceptionHandler handler = beanFactory != null
                    ? beanFactory.getBeanProvider(VirtualTaskExceptionHandler.class)
                            .getIfAvailable(DefaultVirtualTaskExceptionHandler::new)
                    : new DefaultVirtualTaskExceptionHandler();
            return new DefaultVirtualExecutorService(
                    groupName,
                    group.getMaxConcurrency(),
                    group.getMaxPending(),
                    group.getBackpressure(),
                    group.getAcquireTimeout(),
                    properties.getThreadNamePrefix(),
                    handler
            );
        });
        registry.registerBeanDefinition(beanName, definition);
    }

    /**
     * 计算静态业务组对应的 Spring Bean 名称。
     *
     * @param groupName 业务组名称
     * @return Bean 名称
     */
    public static String beanName(String groupName) {
        return groupName + BEAN_SUFFIX;
    }
}
