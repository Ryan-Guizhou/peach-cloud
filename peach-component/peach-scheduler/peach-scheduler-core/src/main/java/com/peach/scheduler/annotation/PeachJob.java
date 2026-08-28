package com.peach.scheduler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 更新相关状态。
 *
 * <p>调度模块说明。
 * 调度模块说明。</p>
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
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    String value();

    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    String description() default "";
}
