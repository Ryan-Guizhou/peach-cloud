package com.peach.mybatis.autoconfigure;

import com.github.pagehelper.PageInterceptor;
import com.peach.mybatis.config.PeachMybatisProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * 注册 PageHelper，并保证它排在慢 SQL 拦截器之前。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@AutoConfiguration
@ConditionalOnClass(PageInterceptor.class)
@ConditionalOnProperty(prefix = "peach.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachMybatisProperties.class)
public class PeachMybatisPageHelperAutoConfiguration {

    @Bean
    @Order(0)
    @ConditionalOnMissingBean(PageInterceptor.class)
    public PageInterceptor peachMybatisPageInterceptor(PeachMybatisProperties properties) {
        PageInterceptor interceptor = new PageInterceptor();
        PeachMybatisProperties.Pagehelper pagehelper = properties.getPagehelper();
        Properties pluginProperties = new Properties();
        if (StringUtils.hasText(pagehelper.getHelperDialect())) {
            pluginProperties.setProperty("helperDialect", pagehelper.getHelperDialect());
        }
        pluginProperties.setProperty("autoRuntimeDialect", Boolean.toString(pagehelper.isAutoRuntimeDialect()));
        pluginProperties.setProperty("reasonable", Boolean.toString(pagehelper.isReasonable()));
        pluginProperties.setProperty("supportMethodsArguments", Boolean.toString(pagehelper.isSupportMethodsArguments()));
        pluginProperties.setProperty("closeConn", Boolean.toString(pagehelper.isCloseConn()));
        interceptor.setProperties(pluginProperties);
        return interceptor;
    }
}
