package com.peach.email.autoconfigure;


import com.peach.email.Idempotency.IdempotencyStore;
import com.peach.email.Idempotency.SimpleIdempotencyStore;
import com.peach.email.constant.EmailConstant;
import com.peach.email.core.EmailContext;
import com.peach.email.retry.RetryPolicy;
import com.peach.email.retry.SimpleRetryPolicy;
import com.peach.email.router.ProviderRouter;
import com.peach.email.service.EmailSendService;
import com.peach.email.smtp.SimpleSmtpConnectionProvider;
import com.peach.email.smtp.SmtpConnectionProvider;
import com.peach.email.smtp.SmtpConnections;
import com.peach.email.smtp.ThreadSmtpConnectionProvider;
import com.peach.email.template.FreeMarkerTemplateRenderer;
import com.peach.email.template.TemplateManager;
import com.peach.email.template.TemplateRenderer;
import com.peach.email.template.TemplateResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:16
 */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean(ProviderRouter.class)
    public ProviderRouter providerRouter(EmailProperties props) {
        ProviderRouter providerRouter = new ProviderRouter();
        registerDefault(providerRouter);
        Map<String, EmailProperties.Provider> providers = props.getProviders();
        Set<Map.Entry<String, EmailProperties.Provider>> entrySet = providers.entrySet();
        Iterator<Map.Entry<String, EmailProperties.Provider>> iterator = entrySet.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, EmailProperties.Provider> nextProvider = iterator.next();
            String name = nextProvider.getKey();
            EmailProperties.Provider provider = providers.get(name);
            String host = Optional.ofNullable(provider.getHost()).orElseGet(() -> defaultHost(name));
            Integer port = Optional.ofNullable( provider.getPort()).orElseGet(() -> defaultPort(name));
            Boolean ssl = Optional.ofNullable(provider.getSsl()).orElseGet(() -> defaultSsl(name));
            validateCredentials(name, provider);
            providerRouter.setContext(name, new EmailContext(host, port, ssl, provider.getUsername(), provider.getPassword(), new Properties()));
            Optional.ofNullable(provider.getPriority()).ifPresent(priority -> providerRouter.setPriority(name, priority));
        }
        return providerRouter;
    }

    @Bean
    @ConditionalOnMissingBean(EmailSendService.class)
    public EmailSendService emailSendService(ProviderRouter router,
                                             IdempotencyStore idempotencyStore,
                                             RetryPolicy retryPolicy,
                                             EmailProperties props) {
        return new EmailSendService(router, idempotencyStore, retryPolicy, props.getDefaultProvider());
    }


    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    public IdempotencyStore idempotencyStore() {
        return new SimpleIdempotencyStore();
    }

    /**
     * 指数退避重试策略：读取属性配置，默认最多3次，基准延迟200ms
     * @param props
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(RetryPolicy.class)
    public RetryPolicy retryPolicy(EmailProperties props) {

        Integer maxAttempts = Optional.ofNullable(props)
                .map(EmailProperties::getRetry)
                .map(EmailProperties.Retry::getMaxAttempts)
                .orElse(EmailConstant.DEFAULT_MAX_ATTEMPTS);

        long baseDelayMillis = Optional.ofNullable( props)
                .map(EmailProperties::getRetry)
                .map(EmailProperties.Retry::getBaseDelayMillis)
                .orElse(EmailConstant.DEFAULT_BASE_DELAY_MILLIS);

        return new SimpleRetryPolicy(maxAttempts,baseDelayMillis);
    }

    @Bean
    @ConditionalOnMissingBean(SmtpConnectionProvider.class)
    public SmtpConnectionProvider smtpConnectionProvider() {
        SmtpConnectionProvider simpleSmtpConnectProvider = new ThreadSmtpConnectionProvider();
        SmtpConnections.setProvider(simpleSmtpConnectProvider);
        return simpleSmtpConnectProvider;
    }

    @Bean
    @ConditionalOnMissingBean(TemplateManager.class)
    public TemplateManager templateManager(TemplateRenderer renderer, ObjectProvider<List<TemplateResolver>> resolversProvider) {
        List<TemplateResolver> resolvers = resolversProvider.getIfAvailable();
        return new TemplateManager(renderer, resolvers);
    }

    @Bean
    @ConditionalOnMissingBean(TemplateRenderer.class)
    public TemplateRenderer templateRenderer() {
        return new FreeMarkerTemplateRenderer();
    }

    /** 注册内置提供商的默认主机端口与SSL */
    private void registerDefault(ProviderRouter router) {
        router.setContext("ali", new EmailContext(defaultHost("ali"), defaultPort("ali"), defaultSsl("ali"), null, null, new Properties()));
        router.setContext("163", new EmailContext(defaultHost("163"), defaultPort("163"), defaultSsl("163"), null, null, new Properties()));
        router.setContext("qq", new EmailContext(defaultHost("qq"), defaultPort("qq"), defaultSsl("qq"), null, null, new Properties()));
        router.setContext("gmail", new EmailContext(defaultHost("gmail"), defaultPort("gmail"), defaultSsl("gmail"), null, null, new Properties()));
    }

    /** 默认主机名 */
    private String defaultHost(String name) {
        if ("ali".equals(name)) return "smtpdm.aliyun.com";
        if ("163".equals(name)) return "smtp.163.com";
        if ("qq".equals(name)) return "smtp.qq.com";
        if ("gmail".equals(name)) return "smtp.gmail.com";
        return "localhost";
    }

    /** 默认端口（SSL 465），可按需改为 587/TLS */
    private int defaultPort(String name) {
        if ("gmail".equals(name)) return 465; // 可切换为587(TLS)
        if ("ali".equals(name)) return 465;
        if ("163".equals(name)) return 465;
        if ("qq".equals(name)) return 465;
        return 25;
    }

    private boolean defaultSsl(String name) { return true; }


    /** 基础校验：用户名域名匹配常见提供商规则 */
    private void validateCredentials(String name, EmailProperties.Provider p) {
        if (p.getUsername() == null || p.getPassword() == null) return; // 允许匿名作为占位
        if ("qq".equals(name)) {
            if (!p.getUsername().endsWith("@qq.com")) throw new IllegalArgumentException("QQ邮箱用户名必须以@qq.com结尾");
        }
        if ("163".equals(name)) {
            if (!p.getUsername().endsWith("@163.com") && !p.getUsername().endsWith("@126.com")) throw new IllegalArgumentException("网易邮箱用户名需@163.com或@126.com");
        }
        if ("gmail".equals(name)) {
            if (!p.getUsername().endsWith("@gmail.com")) throw new IllegalArgumentException("Gmail用户名需@gmail.com");
        }
    }
}
