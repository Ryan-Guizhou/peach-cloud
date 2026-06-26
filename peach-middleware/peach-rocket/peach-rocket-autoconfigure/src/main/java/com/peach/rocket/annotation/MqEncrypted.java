package com.peach.rocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息 payload 加密标记。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MqEncrypted {
}
