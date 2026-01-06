package com.peach.threadpool.annoation;

import com.peach.threadpool.core.PoolType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 18:51
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AsyncExecuted {
    /**
     * 线程池类型
     * @return
     */
    PoolType type() default PoolType.COMMON;

    /**
     * 是否异步
     * @return
     */
    boolean async() default true;

    /**
     * 超时时间
     * @return
     */
    long timeoutMs() default 0L;
}
