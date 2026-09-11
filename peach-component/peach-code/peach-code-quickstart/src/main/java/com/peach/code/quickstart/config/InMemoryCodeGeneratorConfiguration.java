package com.peach.code.quickstart.config;

import com.peach.code.CodeGenerator;
import com.peach.code.quickstart.generator.InMemoryCodeGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

/**
 * 注册内存 {@link CodeGenerator}，覆盖默认 JDBC/Redis 实现，使 quickstart 无需外部存储即可演示发号。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@Indexed
@Configuration
public class InMemoryCodeGeneratorConfiguration {

    /**
     * 提供可重置的内存发号器，同时作为 {@link CodeGenerator} 注入点。
     *
     * @return 内存编码生成器
     */
    @Bean
    public InMemoryCodeGenerator codeGenerator() {
        return new InMemoryCodeGenerator();
    }
}
