package com.peach.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description 配置 OpenAPI 3.0 的所有基本信息项
 */
@Slf4j
@Indexed
@Configuration
public class OpenApiConfig {

    @Value("${peach.openapi.title:认证服务 API}")
    private String title;

    @Value("${peach.openapi.description:PEACH-CLOUD管理系统认证模块 API 接口}")
    private String description;

    @Value("${peach.openapi.version:V1.0.0}")
    private String version;

    @Value("${peach.openapi.contact.name:Ryan_GuiZhou}")
    private String contactName;

    @Value("${peach.openapi.contact.url:https://peachsoft.com}")
    private String contactUrl;

    @Value("${peach.openapi.contact.email:huanhuanshu48@gmail.com}")
    private String contactEmail;

    @Value("${peach.openapi.server-url:}")
    private String serverUrl;

    @Value("${peach.openapi.server-description:网关地址}")
    private String serverDescription;

    @Bean
    public OpenAPI completeOpenApi() {
        OpenAPI openAPI = new OpenAPI()
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
        if (serverUrl != null && !serverUrl.trim().isEmpty()) {
            openAPI.servers(List.of(new Server()
                    .url(serverUrl.trim())
                    .description(serverDescription)));
        }
        return openAPI;
    }

    /**
     * 创建 API 基本信息
     * 包含所有 info 可配置字段
     */
    private Info createApiInfo() {
        Contact contact = new Contact()
                .name(contactName)
                .url(contactUrl)
                .email(contactEmail);

        // 构建 Info 对象
        return new Info()
                .title(title)
                .description(description)
                .version(version)
                .contact(contact);
    }

}
