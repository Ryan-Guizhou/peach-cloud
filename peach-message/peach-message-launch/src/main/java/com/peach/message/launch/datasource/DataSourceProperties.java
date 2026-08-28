package com.peach.message.launch.datasource;

import com.zaxxer.hikari.HikariConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * DataSource配置属性。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 15:47
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "spring.datasource.hikari")
public class DataSourceProperties extends HikariConfig {


}
