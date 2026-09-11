package com.peach.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 QuickStart 仅暴露一个 Spring Boot 应用启动入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 15:14
 */
class QuickstartBootstrapTest {

    @Test
    void shouldExposeSingleSpringBootApplicationEntryPoint() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SpringBootApplication.class));

        Set<BeanDefinition> applications = scanner.findCandidateComponents("com.peach");

        assertThat(applications).hasSize(1);
    }
}
