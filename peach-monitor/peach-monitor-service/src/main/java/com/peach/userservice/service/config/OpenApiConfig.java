package com.peach.userservice.service.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAPI 完整配置类
 * 功能：配置 OpenAPI 3.0 的所有基本信息项（使用 Java 8 语法）
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI completeOpenApi() {
        // 构建 OpenAPI 对象
        return new OpenAPI()
                // 1. API 基本信息
                .info(createApiInfo());
    }

    /**
     * 创建 API 基本信息
     * 包含所有 info 可配置字段
     */
    private Info createApiInfo() {

        Contact contact = new Contact()
                .name("Ryan_GuiZhou")
                .url("https://peachsoft.com")
                .email("huanhuanshu48@gmail.com");

        // 构建 Info 对象
        return new Info()
                .title("监控服务 API")
                .description("PEACH-CLOUD管理系统监控模块 API 接口")
                .version("V1.0.0")
                .contact(contact);
    }

}