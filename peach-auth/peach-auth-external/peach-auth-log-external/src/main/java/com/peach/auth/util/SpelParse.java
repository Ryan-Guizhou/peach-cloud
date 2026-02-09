package com.peach.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/11/13 9:42
 */
@Slf4j
public class SpelParse {

    private StandardEvaluationContext context;

    /**
     * 私有构造器 禁止外部创建实例
     */
    private SpelParse() {

    }

    /**
     * 设置变量
     * @param key
     * @param value
     * @return
     */
    public SpelParse setVariable(String key,Object value){
        context.setVariable(key,value);
        return this;
    }

    /**
     * 初始化上下文
     */
    private void init() {
        context = new StandardEvaluationContext();
    }

    /**
     * 解析表达式
     * @param express
     * @return
     */
    public String parseExpression(String express) {
        try {
            ExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(express);
            return expression.getValue(context, String.class);
        } catch (Exception ex) {
            log.error("An exception occurred while parsing the spell expression"+ex.getMessage(),ex);
            return express;
        }
    }

    /**
     * 创建实例
     * @return
     */
    public static SpelParse create() {
        SpelParse util = new SpelParse();
        util.init();
        return util;
    }
}
