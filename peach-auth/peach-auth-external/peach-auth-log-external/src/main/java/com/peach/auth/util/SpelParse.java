package com.peach.auth.util;

import org.springframework.lang.Nullable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.regex.Pattern;

/**
 * SpEL解析。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
  */
@Slf4j
public class SpelParse {

    private static final int MAX_EXPRESSION_LENGTH = 512;

    private static final Pattern VARIABLE_NAME = Pattern.compile("p\\d{1,3}");

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final StandardEvaluationContext context;

    /**
     * 私有构造器 禁止外部创建实例
     */
    private SpelParse() {
        context = new StandardEvaluationContext();
        context.setBeanResolver(SpelParse::denyBeanAccess);
        context.setTypeLocator(typeName -> {
            throw new EvaluationException("日志表达式不允许访问类型");
        });
        context.setConstructorResolvers(java.util.List.of());
        context.setMethodResolvers(java.util.List.of());
    }

    /**
     * 设置变量
     * @param key
     * @param value
     * @return
     */
    public SpelParse setVariable(String key, Object value) {
        if (key == null || !VARIABLE_NAME.matcher(key).matches()) {
            throw new IllegalArgumentException("非法日志表达式变量名");
        }
        context.setVariable(key,value);
        return this;
    }

    /**
     * 解析表达式
     * @param express
     * @return
     */
    public String parseExpression(String express) {
        if (express == null || express.length() > MAX_EXPRESSION_LENGTH) {
            return express;
        }
        try {
            Expression expression = PARSER.parseExpression(express);
            String result = expression.getValue(context, String.class);
            return result == null ? "" : result;
        } catch (Exception ex) {
            log.warn("Failed to parse operation log expression, keeping template, expressionLength={}", express.length(), ex);
            return express;
        }
    }

    /**
     * 创建实例
     * @return
     */
    public static SpelParse create() {
        return new SpelParse();
    }

    private static Object denyBeanAccess(EvaluationContext evaluationContext, @Nullable String beanName) {
        throw new EvaluationException("日志表达式不允许访问 Bean");
    }
}
