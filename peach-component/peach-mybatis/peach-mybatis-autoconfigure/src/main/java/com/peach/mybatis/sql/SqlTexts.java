package com.peach.mybatis.sql;

import java.util.Locale;

/**
 * SQL 文本判断。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
public final class SqlTexts {

    private SqlTexts() {
    }

    /**
     * 判断 UPDATE / DELETE 在去掉字符串和注释后是否缺少 WHERE。
     *
     * @param sql SQL 原文
     * @return 缺少 WHERE 时返回 true
     */
    public static boolean missingWhere(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String compact = stripLiteralsAndComments(sql).replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        if (!(compact.startsWith("update ") || compact.startsWith("delete "))) {
            return false;
        }
        return !compact.contains(" where ");
    }

    private static String stripLiteralsAndComments(String sql) {
        StringBuilder stripped = new StringBuilder(sql.length());
        int length = sql.length();
        for (int index = 0; index < length; index++) {
            char current = sql.charAt(index);
            if (current == '-' && index + 1 < length && sql.charAt(index + 1) == '-') {
                index = indexOfLineEnd(sql, index) - 1;
                stripped.append(' ');
                continue;
            }
            if (current == '/' && index + 1 < length && sql.charAt(index + 1) == '*') {
                index = indexOfBlockCommentEnd(sql, index + 2) - 1;
                stripped.append(' ');
                continue;
            }
            if (current == '\'' || current == '"') {
                index = indexOfQuotedEnd(sql, index, current) - 1;
                stripped.append(' ');
                continue;
            }
            stripped.append(current);
        }
        return stripped.toString();
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
