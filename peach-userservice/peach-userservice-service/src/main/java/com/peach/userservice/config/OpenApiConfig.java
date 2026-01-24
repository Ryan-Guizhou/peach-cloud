package com.peach.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description 配置 OpenAPI 3.0 的所有基本信息项（使用 Java 8 语法）
 */
@Slf4j
@Indexed
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI completeOpenApi() {
        return new OpenAPI()
                .info(createApiInfo())
                // 全局安全要求（所有接口默认需要 Token）
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                // 安全方案定义
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .name("全局请求token")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Sa-Token 登录凭证")
                        ).addSecuritySchemes("User-Agent",new SecurityScheme()
                                .name("全局请求User-Agent")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("User-Agent 用户代理"))
                        .addSecuritySchemes("Referer",new SecurityScheme()
                                .name("全局请求Referer")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Referer 引用来源"))
                );
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
                .title("用户服务 API")
                .description("PEACH-CLOUD管理系统用户模块 API 接口")
                .version("V1.0.0")
                .contact(contact);
    }

}