package com.peach.mongo.log;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mongo 命令的两种互斥文本。
 * <p>placeholder 把用户数据换成 {@code ?}，parameters 按出现顺序列出这些值。
 * executable 是可以在 mongosh 执行的 {@code db.runCommand(...)}。
 * 调用方只能选择其中一种写入日志。</p>
 *
 * @param placeholder 保留集合名和驱动字段、用户数据改为 {@code ?} 的命令
 * @param parameters 与 {@code ?} 顺序一致的参数说明
 * @param executable 参数已内联的可执行命令
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:54
 */
public record MongoCommandText(String placeholder, String parameters, String executable) {

    /**
     * 这些字段的值是业务数据，需要换成 {@code ?}。
     * limit、lsid、$db 等驱动控制字段保持原样，避免把会话信息记进 Parameters。
     */
    private static final Set<String> USER_VALUE_KEYS = Set.of(
            "filter", "query", "q", "u", "projection", "sort", "hint",
            "pipeline", "let", "arrayFilters", "document");

    /**
     * 这些字段只有在值是文档时才视为用户数据。
     * {@code update} 命令里同名的值是集合名字符串，必须保持原样。
     */
    private static final Set<String> USER_DOCUMENT_KEYS = Set.of("update", "fields");

    private static final Set<String> USER_DOCUMENT_ARRAY_KEYS = Set.of("documents");

    /**
     * 只生成带 {@code ?} 的命令和参数行，不拼可执行命令。
     *
     * @param command 驱动发出的命令，允许为 null
     * @return 占位文本和参数
     */
    public static Placeholder placeholder(BsonDocument command) {
        List<String> values = new ArrayList<>();
        String text = command == null ? "{}" : renderDocument(command, Scope.STRUCTURAL, values);
        return new Placeholder(text, String.join(", ", values));
    }

    /**
     * 同时生成两种文本，供测试对照。日志输出只能选用其中一种。
     *
     * @param command 驱动发出的命令，允许为 null
     * @return 占位文本、参数和可执行命令
     */
    public static MongoCommandText of(BsonDocument command) {
        Placeholder placeholder = placeholder(command);
        return new MongoCommandText(placeholder.text(), placeholder.parameters(), MongoCommands.executable(command));
    }

    /**
     * 带 {@code ?} 的命令文本。text 与 parameters 成对出现，不包含可执行命令。
     *
     * @param text 用户数据已替换为 {@code ?} 的命令
     * @param parameters 与 {@code ?} 顺序一致的参数说明
     */
    public record Placeholder(String text, String parameters) {
    }

    private static String renderDocument(BsonDocument document, Scope scope, List<String> values) {
        if (document.isEmpty()) {
            return "{}";
        }
        StringBuilder rendered = new StringBuilder("{ ");
        boolean first = true;
        for (var entry : document.entrySet()) {
            if (!first) {
                rendered.append(", ");
            }
            first = false;
            rendered.append(quote(entry.getKey())).append(" : ");
            renderValue(entry.getKey(), entry.getValue(), scope, values, rendered);
        }
        rendered.append(" }");
        return rendered.toString();
    }

    private static void renderValue(String key, BsonValue value, Scope scope, List<String> values, StringBuilder rendered) {
        Scope childScope = childScope(key, value, scope);
        if (value == null || value.isNull()) {
            writeScalar(null, childScope, values, rendered);
            return;
        }
        if (value.isDocument()) {
            rendered.append(renderDocument(value.asDocument(), childScope, values));
            return;
        }
        if (value.isArray()) {
            renderArray(key, value.asArray(), childScope, values, rendered);
            return;
        }
        writeScalar(value, childScope, values, rendered);
    }

    private static void renderArray(String key, BsonArray array, Scope scope, List<String> values, StringBuilder rendered) {
        Scope elementScope = USER_DOCUMENT_ARRAY_KEYS.contains(key) ? Scope.USER : scope;
        rendered.append("[ ");
        for (int index = 0; index < array.size(); index++) {
            if (index > 0) {
                rendered.append(", ");
            }
            BsonValue element = array.get(index);
            if (element.isDocument()) {
                rendered.append(renderDocument(element.asDocument(), elementScope, values));
            } else if (element.isArray()) {
                renderArray(key, element.asArray(), elementScope, values, rendered);
            } else {
                writeScalar(element.isNull() ? null : element, elementScope, values, rendered);
            }
        }
        rendered.append(array.isEmpty() ? "]" : " ]");
    }

    /**
     * 结构字段写原始 JSON。进入用户数据范围后，标量一律换成 {@code ?} 并记入参数。
     */
    private static void writeScalar(BsonValue value, Scope scope, List<String> values, StringBuilder rendered) {
        if (scope == Scope.STRUCTURAL) {
            rendered.append(value == null ? "null" : literal(value));
            return;
        }
        rendered.append('?');
        values.add(value == null ? "null" : describe(value));
    }

    private static Scope childScope(String key, BsonValue value, Scope scope) {
        if (scope == Scope.USER || USER_VALUE_KEYS.contains(key)) {
            return Scope.USER;
        }
        if (value != null && value.isDocument() && USER_DOCUMENT_KEYS.contains(key)) {
            return Scope.USER;
        }
        return Scope.STRUCTURAL;
    }

    private static String literal(BsonValue value) {
        if (value.isString()) {
            return quote(value.asString().getValue());
        }
        if (value.isInt32()) {
            return Integer.toString(value.asInt32().getValue());
        }
        if (value.isInt64()) {
            return Long.toString(value.asInt64().getValue());
        }
        if (value.isDouble()) {
            return Double.toString(value.asDouble().getValue());
        }
        if (value.isBoolean()) {
            return Boolean.toString(value.asBoolean().getValue());
        }
        if (value.isObjectId()) {
            return "{\"$oid\": \"" + value.asObjectId().getValue().toHexString() + "\"}";
        }
        return String.valueOf(value);
    }

    private static String describe(BsonValue value) {
        if (value.isString()) {
            return value.asString().getValue() + "(String)";
        }
        if (value.isInt32()) {
            return value.asInt32().getValue() + "(Int32)";
        }
        if (value.isInt64()) {
            return value.asInt64().getValue() + "(Int64)";
        }
        if (value.isDouble()) {
            return value.asDouble().getValue() + "(Double)";
        }
        if (value.isBoolean()) {
            return value.asBoolean().getValue() + "(Boolean)";
        }
        if (value.isObjectId()) {
            return value.asObjectId().getValue().toHexString() + "(ObjectId)";
        }
        if (value.isDateTime()) {
            return value.asDateTime().getValue() + "(DateTime)";
        }
        return String.valueOf(value);
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private enum Scope {
        /** 集合名、limit、lsid 等保持字面量。 */
        STRUCTURAL,
        /** filter、q、u、插入文档中的值改为 ?。 */
        USER
    }
}
