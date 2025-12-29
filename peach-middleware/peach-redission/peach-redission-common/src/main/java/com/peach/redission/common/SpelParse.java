package com.peach.redission.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 10:01
 */
@Slf4j
public class SpelParse {

    private StandardEvaluationContext context;

    private SpelParse() {

    }

    public static SpelParse create(){
        SpelParse spelParse = new SpelParse();
        spelParse.init();
        return spelParse;
    }

    private void init(){
        context = new StandardEvaluationContext();
    }

    public void setVariable(String name,Object value){
        context.setVariable(name,value);
    }

    /**
     * 解析表达式获取值
     * @param expression
     * @return
     */
    public String parseExpression(String expressionString){
        try {
            ExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(expressionString);
            return expression.getValue(context, String.class);
        } catch (Exception ex) {
            log.error("An exception occurred while parsing the spell expression" + ex);
            return expressionString;
        }
    }
}
