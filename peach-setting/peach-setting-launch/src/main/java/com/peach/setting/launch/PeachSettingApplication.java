package com.peach.setting.launch;

import com.peach.common.annoation.MybatisDao;
import com.peach.setting.launch.datasource.DataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import java.io.IOException;

import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * setting 服务启动类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description setting 服务启动类
 */
@Slf4j
@EnableCaching
@EnableDiscoveryClient
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(DataSourceProperties.class)
@MapperScan(basePackages = {"com.peach.*.dao"}, annotationClass = MybatisDao.class, sqlSessionFactoryRef = "mybatisSession")
@SpringBootApplication(scanBasePackages = "com.peach")
public class PeachSettingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachSettingApplication.class, args);
    }


    @Primary
    @Bean("dataSource")
    public DataSource dataSource(DataSourceProperties properties) {
        // 禁止自动提交
        properties.setAutoCommit(false);
        return new HikariDataSource(properties);
    }

    @Primary
    @Bean("mybatisSession")
    public SqlSessionFactoryBean sqlSessionFactoryBean(@Qualifier("dataSource") DataSource dataSource) throws IOException {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setConfigLocation(new ClassPathResource("mybatis-config.xml"));
        sqlSessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:com/peach/**/*.xml")
        );
        return sqlSessionFactoryBean;
    }

    @Primary
    @Bean("transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
