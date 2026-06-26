package com.peach.rocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MQ 事件路由注解。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MqEvent {

    /**
     * 业务 topic，开启命名规范后会被补齐命名空间和前缀。
     *
     * @return topic 名称
     */
    String topic();

    /**
     * 消息 tag。
     *
     * @return tag 名称
     */
    String tag() default "";

    /**
     * 业务 key，支持 SpEL 表达式。
     *
     * @return 业务 key 或 SpEL 表达式
     */
    String key() default "";

    /**
     * 事件版本。
     *
     * @return 事件版本号
     */
    int version() default 1;
}
