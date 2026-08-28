package com.peach.fileservice.launch;

import com.peach.common.annoation.MybatisDao;
import com.peach.fileservice.launch.datasource.DataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import java.io.IOException;

import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * PeachFileservice启动类。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/25 16:02
 */
@SpringBootApplication
@ComponentScan({"com.peach"})
@MapperScan(basePackages = {"com.peach.*.dao"},
        annotationClass = MybatisDao.class,
        sqlSessionFactoryRef = "mybatisSession")
@EnableConfigurationProperties(DataSourceProperties.class)
public class PeachFileserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachFileserviceApplication.class, args);
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
    public SqlSessionFactoryBean sqlSessionFactoryBean(@Qualifier("dataSource") DataSource dataSource,
                                                       DatabaseIdProvider databaseIdProvider) throws IOException {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setDatabaseIdProvider(databaseIdProvider);
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

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("Oracle", "oracle");
        properties.setProperty("DM", "mysql");
        properties.setProperty("PostgreSQL", "postgresql");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }
}
