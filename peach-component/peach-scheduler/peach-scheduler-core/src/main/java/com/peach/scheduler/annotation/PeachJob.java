package com.peach.scheduler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记可被 Peach Scheduler 注册和调用的业务任务处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PeachJob {

    /**
     * 任务处理器名称，必须与调度定义中的 handlerName 保持一致。
     *
     * @return 任务处理器名称。
     */
    String value();

    /**
     * 任务处理器说明，用于注册信息展示和排障。
     *
     * @return 任务处理器说明。
     */
    String description() default "";
}
