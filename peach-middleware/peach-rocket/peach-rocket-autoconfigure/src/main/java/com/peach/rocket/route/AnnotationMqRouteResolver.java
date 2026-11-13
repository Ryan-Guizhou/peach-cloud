package com.peach.rocket.route;

import com.peach.rocket.annotation.MqEvent;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.support.RocketMqNaming;
import java.beans.PropertyDescriptor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

/**
 * 基于业务注解与单次发送选项解析 Topic、Tag、Key 的默认路由实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class AnnotationMqRouteResolver implements MqRouteResolver {

    private final PeachRocketProperties properties;

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public AnnotationMqRouteResolver(PeachRocketProperties properties) {
        this.properties = properties;
    }

    /**
     * 根据单次覆盖参数优先、事件注解次之的顺序解析业务路由。
     *
     * @param payload 业务消息体，不允许为 null
     * @param options 业务发送选项，可为空
     * @return 标准化后的 RocketMQ 路由
     */
    @Override
    public MqRoute resolve(Object payload, MqSendOptions options) {
        if (payload == null) {
            throw new MqException("MQ payload must not be null");
        }
        MqSendOptions actualOptions = MqSendOptions.copyOf(options);
        MqEvent event = payload.getClass().getAnnotation(MqEvent.class);
        String topic = resolveTopic(actualOptions, event);
        if (!StringUtils.hasText(topic)) {
            throw new MqException("MQ topic must not be blank");
        }
        String tag = resolveTag(actualOptions, event);
        String keyExpression = resolveKeyExpression(actualOptions, event);
        String key = resolveKey(payload, keyExpression);
        return new MqRoute(RocketMqNaming.normalizeTopic(topic, properties), tag, key);
    }

    private String resolveTopic(MqSendOptions options, MqEvent event) {
        if (StringUtils.hasText(options.getTopic())) {
            return options.getTopic();
        }
        return event == null ? null : event.topic();
    }

    private String resolveTag(MqSendOptions options, MqEvent event) {
        if (StringUtils.hasText(options.getTag())) {
            return options.getTag();
        }
        return event == null ? null : event.tag();
    }

    private String resolveKeyExpression(MqSendOptions options, MqEvent event) {
        if (StringUtils.hasText(options.getKey())) {
            return options.getKey();
        }
        return event == null ? null : event.key();
    }

    private String resolveKey(Object payload, String keyExpression) {
        if (!StringUtils.hasText(keyExpression)) {
            return null;
        }
        if (!keyExpression.startsWith("#")) {
            return keyExpression;
        }
        StandardEvaluationContext context = new StandardEvaluationContext(payload);
        BeanWrapper beanWrapper = new BeanWrapperImpl(payload);
        for (PropertyDescriptor descriptor : beanWrapper.getPropertyDescriptors()) {
            String name = descriptor.getName();
            if (beanWrapper.isReadableProperty(name)) {
                context.setVariable(name, beanWrapper.getPropertyValue(name));
            }
        }
        Object value = expressionParser.parseExpression(keyExpression).getValue(context);
        return value == null ? null : String.valueOf(value);
    }
}
