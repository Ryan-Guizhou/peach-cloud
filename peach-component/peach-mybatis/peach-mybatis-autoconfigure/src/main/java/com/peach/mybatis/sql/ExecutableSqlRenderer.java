package com.peach.mybatis.sql;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;

/**
 * 把带 {@code ?} 的 SQL 渲染成可执行语句。
 * <p>只在 {@code peach.mybatis.log.executable=true} 时使用。默认日志保留 {@code ?} 和 Parameters，
 * 不会和这里的结果一起输出。字符串、标识符引号和注释中的问号不会被替换。
 * 布尔值写成 true/false，二进制写成 X'...'。MySQL 和 PostgreSQL 可以直接执行这两种字面量，
 * Oracle 的写法不同。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
public final class ExecutableSqlRenderer {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ExecutableSqlRenderer() {
    }

    /**
     * 按顺序替换问号占位符。字符串、注释里的问号保持原样。
     *
     * @param sql SQL 原文
     * @param values 与占位符顺序一致的参数
     * @return 可执行 SQL
     */
    public static String render(String sql, List<?> values) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        List<?> arguments = values == null ? List.of() : values;
        StringBuilder rendered = new StringBuilder(sql.length() + arguments.size() * 8);
        int argumentIndex = 0;
        int length = sql.length();
        for (int index = 0; index < length; index++) {
            char current = sql.charAt(index);
            if (current == '-' && index + 1 < length && sql.charAt(index + 1) == '-') {
                int end = indexOfLineEnd(sql, index);
                rendered.append(sql, index, end);
                index = end - 1;
                continue;
            }
            if (current == '/' && index + 1 < length && sql.charAt(index + 1) == '*') {
                int end = indexOfBlockCommentEnd(sql, index + 2);
                rendered.append(sql, index, end);
                index = end - 1;
                continue;
            }
            if (current == '\'' || current == '"') {
                int end = indexOfQuotedEnd(sql, index, current);
                rendered.append(sql, index, end);
                index = end - 1;
                continue;
            }
            if (current == '?') {
                Object value = argumentIndex < arguments.size() ? arguments.get(argumentIndex) : null;
                argumentIndex++;
                rendered.append(literal(value));
                continue;
            }
            rendered.append(current);
        }
        return rendered.toString();
    }

    /**
     * 把单个参数格式化成 SQL 字面量。
     *
     * @param value 参数值
     * @return SQL 字面量
     */
    public static String literal(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "true" : "false";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Timestamp timestamp) {
            return quote(DATE_TIME.format(timestamp.toLocalDateTime()));
        }
        if (value instanceof Time time) {
            return quote(time.toLocalTime().toString());
        }
        if (value instanceof java.sql.Date date) {
            return quote(date.toLocalDate().toString());
        }
        if (value instanceof Date date) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
            return quote(DATE_TIME.format(dateTime));
        }
        if (value instanceof LocalDateTime dateTime) {
            return quote(DATE_TIME.format(dateTime));
        }
        if (value instanceof LocalDate || value instanceof LocalTime) {
            return quote(value.toString());
        }
        if (value instanceof TemporalAccessor) {
            return quote(value.toString());
        }
        if (value instanceof byte[] bytes) {
            return "X'" + hex(bytes) + "'";
        }
        if (value instanceof Enum<?> enumValue) {
            return quote(enumValue.name());
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String text) {
        return "'" + text.replace("'", "''") + "'";
    }

    private static String hex(byte[] bytes) {
        byte[] encoded = new byte[bytes.length * 2];
        int position = 0;
        for (byte current : bytes) {
            int value = current & 0xFF;
            encoded[position++] = (byte) Character.toUpperCase(Character.forDigit(value >>> 4, 16));
            encoded[position++] = (byte) Character.toUpperCase(Character.forDigit(value & 0x0F, 16));
        }
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    private static int indexOfLineEnd(String sql, int from) {
        int newline = sql.indexOf('\n', from);
        return newline < 0 ? sql.length() : newline;
    }

    private static int indexOfBlockCommentEnd(String sql, int from) {
        int end = sql.indexOf("*/", from);
        return end < 0 ? sql.length() : end + 2;
    }

    private static int indexOfQuotedEnd(String sql, int openIndex, char quote) {
        for (int index = openIndex + 1; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == '\\' && index + 1 < sql.length()) {
                index++;
                continue;
            }
            if (current == quote) {
                if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                    index++;
                    continue;
                }
                return index + 1;
            }
        }
        return sql.length();
    }
}
