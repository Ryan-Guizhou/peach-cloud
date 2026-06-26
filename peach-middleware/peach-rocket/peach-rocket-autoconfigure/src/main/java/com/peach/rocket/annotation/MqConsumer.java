package com.peach.rocket.annotation;

import com.peach.rocket.core.MqConsumeMode;
import com.peach.rocket.core.MqMessageModel;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态 MQ 消费者声明。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MqConsumer {

    /**
     * 业务 topic。
     *
     * @return topic 名称
     */
    String topic();

    /**
     * tag 过滤表达式，空值表示订阅全部 tag。
     *
     * @return tag 表达式
     */
    String tag() default "";

    /**
     * 消费者组。
     *
     * @return consumer group
     */
    String consumerGroup();

    /**
     * 消费模式。
     *
     * @return 并发或顺序消费
     */
    MqConsumeMode consumeMode() default MqConsumeMode.CONCURRENTLY;

    /**
     * 消息模型。
     *
     * @return 集群或广播消费
     */
    MqMessageModel messageModel() default MqMessageModel.CLUSTERING;

    /**
     * 最大重试次数，小于 0 表示使用 RocketMQ 默认值。
     *
     * @return 最大重试次数
     */
    int maxReconsumeTimes() default -1;

    /**
     * 是否启用消费幂等保护。
     *
     * @return true 表示启用幂等保护
     */
    boolean idempotent() default true;
}
