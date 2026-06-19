package com.peach.setting.config;

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
 * @Description OpenAPI 3.0 配置
 */
@Slf4j
@Indexed
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI completeOpenApi() {
        return new OpenAPI()
                .info(createApiInfo())
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .name("Authorization")
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
    }

    /**
     * 创建 API 信息
     *
     * @return OpenAPI 信息对象
     */
    private Info createApiInfo() {
        Contact contact = new Contact()
                .name("Ryan_GuiZhou")
                .url("https://peachsoft.com")
                .email("huanhuanshu48@gmail.com");

        return new Info()
                .title("PEACH-CLOUD 设置中心 API")
                .description("PEACH-CLOUD 设置中心 API")
                .version("V1.0.0")
                .contact(contact);
    }
}
