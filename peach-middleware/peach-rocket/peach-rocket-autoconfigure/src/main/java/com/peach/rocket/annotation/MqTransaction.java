package com.peach.rocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RocketMQ 事务消息处理器路由注解。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MqTransaction {

    /**
     * 业务 topic。
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
}
