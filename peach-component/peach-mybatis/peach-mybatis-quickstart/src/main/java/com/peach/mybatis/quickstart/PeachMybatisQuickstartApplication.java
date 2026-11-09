package com.peach.mybatis.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Peach MyBatis Quickstart 启动入口。
 * <p>示例只验证 starter 装配，排除数据源自动配置，不连接数据库。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class PeachMybatisQuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachMybatisQuickstartApplication.class, args);
    }
}
