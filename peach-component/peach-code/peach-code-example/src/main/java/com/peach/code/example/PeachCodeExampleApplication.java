package com.peach.code.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Peach代码Example启动类。
 * <p>配置可访问的测试 MySQL 与 Redis 后启动应用，由 {@link PeachCodeEvent} 输出实际发号及格式校验结果。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@SpringBootApplication
public class PeachCodeExampleApplication {

    /**
     * 启动业务编码生成器示例应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PeachCodeExampleApplication.class, args);
    }
}
