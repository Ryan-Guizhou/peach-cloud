package com.peach.virtualthread.annotation;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按业务组注入 {@code VirtualExecutorService} 的限定注解。
 *
 * <p>业务代码应优先通过该注解表达资源组语义，而不是直接依赖 Spring Bean 名称。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:20
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface VirtualGroup {

    /**
     * 业务组名称。
     *
     * @return 业务组名称
     */
    String value();
}
