package com.peach.userservice.launch.datasource;

import com.zaxxer.hikari.HikariConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 15:47
 */
@Data
@ConfigurationProperties(prefix = "spring.datasource.hikari")
public class DataSourceProperties extends HikariConfig {


}
