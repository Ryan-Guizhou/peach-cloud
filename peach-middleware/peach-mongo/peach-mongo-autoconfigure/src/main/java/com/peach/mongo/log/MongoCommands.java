package com.peach.mongo.log;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.util.Locale;

/**
 * Mongo 命令的标识、可执行文本和危险写入判断。
 * <p>带 {@code ?} 的占位文本由 {@link MongoCommandText} 生成。本类的 {@link #executable(BsonDocument)}
 * 只在 {@code peach.mongo.log.executable=true} 时单独输出，不与占位文本同时出现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
public final class MongoCommands {

    private MongoCommands() {
    }

    /**
     * 生成日志里的语句标识，格式为库名.集合.命令。
     *
     * @param database 库名
     * @param commandName 命令名
     * @param command 命令文档
     * @return 语句标识
     */
    public static String statementId(String database, String commandName, BsonDocument command) {
        String collection = collection(commandName, command);
        if (collection == null || collection.isBlank()) {
            return database + "." + commandName;
        }
        return database + "." + collection + "." + commandName;
    }

    /**
     * 从命令文档中取集合名。
     *
     * @param commandName 命令名
     * @param command 命令文档
     * @return 集合名，没有时返回 null
     */
    public static String collection(String commandName, BsonDocument command) {
        if (command == null) {
            return null;
        }
        BsonValue named = command.get(commandName);
        if (named != null && named.isString()) {
            return named.asString().getValue();
        }
        BsonValue collection = command.get("collection");
        if (collection != null && collection.isString()) {
            return collection.asString().getValue();
        }
        return null;
    }

    /**
     * 拼出可以在 mongosh 中直接执行的命令。参数保留在 JSON 里，不使用 {@code ?}。
     *
     * @param command 命令文档，允许为 null
     * @return {@code db.runCommand(...)}
     */
    public static String executable(BsonDocument command) {
        if (command == null) {
            return "db.runCommand({})";
        }
        return "db.runCommand(" + command.toJson() + ")";
    }

    /**
     * update / delete 的过滤条件为空时返回 true。
     *
     * @param commandName 命令名
     * @param command 命令文档
     * @return 是否缺少过滤条件
     */
    public static boolean emptyWriteFilter(String commandName, BsonDocument command) {
        if (commandName == null || command == null) {
            return false;
        }
        String name = commandName.toLowerCase(Locale.ROOT);
        if ("delete".equals(name)) {
            return arrayHasEmptyQuery(command, "deletes");
        }
        if ("update".equals(name)) {
            return arrayHasEmptyQuery(command, "updates");
        }
        if ("findandmodify".equals(name)) {
            return documentEmpty(command.get("query"));
        }
        return false;
    }

    /**
     * 读取 find / aggregate 首批返回文档数。
     *
     * @param response 命令响应
     * @return 首批条数，无法识别时返回 0
     */
    public static int resultSize(BsonDocument response) {
        if (response == null) {
            return 0;
        }
        BsonValue cursor = response.get("cursor");
        if (cursor == null || !cursor.isDocument()) {
            return 0;
        }
        BsonValue batch = cursor.asDocument().get("firstBatch");
        if (batch != null && batch.isArray()) {
            return batch.asArray().size();
        }
        return 0;
    }

    private static boolean arrayHasEmptyQuery(BsonDocument command, String field) {
        BsonValue items = command.get(field);
        if (items == null || !items.isArray()) {
            return false;
        }
        for (BsonValue item : items.asArray()) {
            if (!item.isDocument()) {
                continue;
            }
            if (documentEmpty(item.asDocument().get("q"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean documentEmpty(BsonValue value) {
        return value == null || !value.isDocument() || value.asDocument().isEmpty();
    }
}
