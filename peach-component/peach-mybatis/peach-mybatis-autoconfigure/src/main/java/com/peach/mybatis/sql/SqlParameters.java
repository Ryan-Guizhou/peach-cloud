package com.peach.mybatis.sql;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 BoundSql 中按占位符顺序取出参数值。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
public final class SqlParameters {

    private SqlParameters() {
    }

    /**
     * 读取与 SQL 问号顺序一致的参数值。
     *
     * @param mappedStatement 当前语句
     * @param boundSql 绑定后的 SQL
     * @return 参数值列表
     */
    public static List<Object> values(MappedStatement mappedStatement, BoundSql boundSql) {
        List<Object> values = new ArrayList<>();
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        if (mappings == null || mappings.isEmpty()) {
            return values;
        }
        Configuration configuration = mappedStatement.getConfiguration();
        Object parameterObject = boundSql.getParameterObject();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        for (ParameterMapping mapping : mappings) {
            if (mapping.getMode() == ParameterMode.OUT) {
                continue;
            }
            values.add(read(configuration, boundSql, typeHandlerRegistry, parameterObject, mapping.getProperty()));
        }
        return values;
    }

    /**
     * 把参数格式化成 Parameters 行。顺序与 SQL 中的 {@code ?} 一致，null 输出为 null。
     *
     * @param values 参数值
     * @return 例如 {@code 1(Integer), peach(String)}
     */
    public static String format(List<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                formatted.append(", ");
            }
            Object value = values.get(index);
            if (value == null) {
                formatted.append("null");
                continue;
            }
            formatted.append(value).append('(').append(value.getClass().getSimpleName()).append(')');
        }
        return formatted.toString();
    }

    private static Object read(Configuration configuration, BoundSql boundSql, TypeHandlerRegistry typeHandlerRegistry,
                               Object parameterObject, String property) {
        if (boundSql.hasAdditionalParameter(property)) {
            return boundSql.getAdditionalParameter(property);
        }
        if (parameterObject == null) {
            return null;
        }
        if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            return parameterObject;
        }
        MetaObject metaObject = configuration.newMetaObject(parameterObject);
        if (!metaObject.hasGetter(property)) {
            return null;
        }
        return metaObject.getValue(property);
    }
}
