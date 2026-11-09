package com.peach.mybatis.autoconfigure;

import com.peach.mybatis.config.PeachMybatisProperties;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 把手写的 {@code SqlSessionFactoryBean} 接上 databaseId 与拦截器。
 * <p>MyBatis 官方自动配置只在自己创建工厂时收集容器里的拦截器。
 * 业务启动类自己 new 出来的工厂不会经过那条路径，所以在初始化前写入
 * {@code DatabaseIdProvider} 和已排序的拦截器。耗时拦截器的顺序值更大，注册在 PageHelper 之后，
 * 因此它位于外层：先记录耗时并读取分页改写前的 SQL，再进入 PageHelper。
 * {@code peach.mybatis.jdbc-type-for-null=true} 时，工厂创建完成后再把空参数的 JDBC 类型设为 NULL。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
public class PeachMybatisSqlSessionFactoryPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private final PeachMybatisProperties properties;

    private BeanFactory beanFactory;

    public PeachMybatisSqlSessionFactoryPostProcessor(PeachMybatisProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SqlSessionFactoryBean factory) {
            factory.setDatabaseIdProvider(beanFactory.getBean(DatabaseIdProvider.class));
            Interceptor[] plugins = beanFactory.getBeanProvider(Interceptor.class)
                    .orderedStream()
                    .toArray(Interceptor[]::new);
            if (plugins.length > 0) {
                factory.setPlugins(plugins);
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!properties.isJdbcTypeForNull()) {
            return bean;
        }
        if (bean instanceof SqlSessionFactory sessionFactory) {
            sessionFactory.getConfiguration().setJdbcTypeForNull(JdbcType.NULL);
        }
        if (bean instanceof SqlSessionFactoryBean factoryBean) {
            applyJdbcTypeForNull(factoryBean);
        }
        return bean;
    }

    private static void applyJdbcTypeForNull(SqlSessionFactoryBean factoryBean) {
        try {
            SqlSessionFactory sessionFactory = factoryBean.getObject();
            if (sessionFactory != null) {
                sessionFactory.getConfiguration().setJdbcTypeForNull(JdbcType.NULL);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to set jdbcTypeForNull", exception);
        }
    }
}
