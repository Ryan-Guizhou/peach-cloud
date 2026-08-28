package com.peach.setting.config;

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
 * @Description OpenAPI 3.0 配置
 */
@Slf4j
@Indexed
@Configuration
public class OpenApiConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";


    @Value("${peach.openapi.title:PEACH-CLOUD 设置中心 API}")
    private String title;

    @Value("${peach.openapi.description:PEACH-CLOUD 设置中心 API}")
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
                .addSecurityItem(new SecurityRequirement().addList(AUTHORIZATION_HEADER))
                .components(new Components()
                        .addSecuritySchemes(AUTHORIZATION_HEADER,
                                new SecurityScheme()
                                        .name(AUTHORIZATION_HEADER)
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Sa-Token 认证"))
                        .addSecuritySchemes("User-Agent",
                                new SecurityScheme()
                                        .name("User-Agent")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("User-Agent 请求头"))
                        .addSecuritySchemes("Referer",
                                new SecurityScheme()
                                        .name("Referer")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Referer 来源头")));
        if (serverUrl != null && !serverUrl.isBlank()) {
            openAPI.servers(List.of(new Server()
                    .url(serverUrl.trim())
                    .description(serverDescription)));
        }
        return openAPI;
    }

    /**
     * 创建 API 信息
     *
     * @return OpenAPI 信息对象
     */
    private Info createApiInfo() {
        Contact contact = new Contact()
                .name(contactName)
                .url(contactUrl)
                .email(contactEmail);

        return new Info()
                .title(title)
                .description(description)
                .version(version)
                .contact(contact);
    }
}
