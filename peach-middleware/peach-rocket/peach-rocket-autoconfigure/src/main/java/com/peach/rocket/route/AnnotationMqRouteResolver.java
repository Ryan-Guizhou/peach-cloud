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
 * 基于 {@link MqEvent} 的 MQ 路由解析器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class AnnotationMqRouteResolver implements MqRouteResolver {

    private final PeachRocketProperties properties;

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    public AnnotationMqRouteResolver(PeachRocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public MqRoute resolve(Object payload, MqSendOptions options) {
        if (payload == null) {
            throw new MqException("MQ payload must not be null");
        }
        MqEvent event = payload.getClass().getAnnotation(MqEvent.class);
        String topic = resolveTopic(options, event);
        if (!StringUtils.hasText(topic)) {
            throw new MqException("MQ topic must not be blank");
        }
        String tag = resolveTag(options, event);
        String keyExpression = resolveKeyExpression(options, event);
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
