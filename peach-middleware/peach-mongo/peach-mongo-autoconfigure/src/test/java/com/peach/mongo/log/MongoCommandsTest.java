package com.peach.mongo.log;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mongo 命令文本。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
class MongoCommandsTest {

    @Test
    void shouldBuildExecutableStatementId() {
        BsonDocument command = BsonDocument.parse("{\"find\": \"audit_log\", \"filter\": {\"actor\": \"it\"}}");

        MongoCommandText text = MongoCommandText.of(command);

        assertThat(MongoCommands.statementId("peach_qs", "find", command)).isEqualTo("peach_qs.audit_log.find");
        assertThat(text.placeholder()).contains("\"find\" : \"audit_log\"");
        assertThat(text.placeholder()).contains("\"actor\" : ?");
        assertThat(text.placeholder()).doesNotContain("\"it\"");
        assertThat(text.parameters()).isEqualTo("String");
        assertThat(text.parameters()).doesNotContain("it");
        assertThat(text.executable()).startsWith("db.runCommand(");
        assertThat(text.executable()).contains("\"actor\": \"it\"");
        assertThat(text.executable()).doesNotContain("?");
    }

    @Test
    void shouldKeepDriverFieldsLiteralAndMaskWriteValues() {
        BsonDocument delete = BsonDocument.parse(
                "{\"delete\": \"audit_log\", \"deletes\": [{\"q\": {\"actor\": \"it\"}, \"limit\": 1}]}");
        MongoCommandText text = MongoCommandText.of(delete);

        assertThat(text.placeholder()).contains("\"limit\" : 1");
        assertThat(text.placeholder()).contains("?");
        assertThat(text.parameters()).isEqualTo("it(String)");
        assertThat(text.executable()).doesNotContain("?");
    }

    @Test
    void shouldDetectEmptyWriteFilterAndResultSize() {
        BsonDocument delete = BsonDocument.parse("{\"delete\": \"audit_log\", \"deletes\": [{\"q\": {}, \"limit\": 0}]}");
        BsonDocument update = BsonDocument.parse("{\"update\": \"audit_log\", \"updates\": [{\"q\": {\"_id\": 1}, \"u\": {\"$set\": {\"a\": 1}}}]}");
        BsonDocument response = BsonDocument.parse("{\"cursor\": {\"firstBatch\": [{}, {}]}, \"ok\": 1}");

        assertThat(MongoCommands.emptyWriteFilter("delete", delete)).isTrue();
        assertThat(MongoCommands.emptyWriteFilter("update", update)).isFalse();
        assertThat(MongoCommands.resultSize(response)).isEqualTo(2);
    }

    @Test
    void shouldMaskFindAndModifyUpdateWithoutMaskingCollectionName() {
        BsonDocument modify = BsonDocument.parse(
                "{\"findAndModify\":\"audit_log\",\"query\":{\"actor\":\"it\"},\"update\":{\"$set\":{\"name\":\"peach\"}},\"fields\":{\"name\":1}}");
        BsonDocument update = BsonDocument.parse(
                "{\"update\":\"audit_log\",\"updates\":[{\"q\":{\"_id\":1},\"u\":{\"$set\":{\"a\":2}}}]}");

        MongoCommandText.Placeholder modifyText = MongoCommandText.placeholder(modify);
        MongoCommandText.Placeholder updateText = MongoCommandText.placeholder(update);

        assertThat(modifyText.text()).contains("\"findAndModify\" : \"audit_log\"");
        assertThat(modifyText.text()).doesNotContain("peach");
        assertThat(modifyText.parameters()).isEqualTo("String, String, Int32");
        assertThat(updateText.text()).contains("\"update\" : \"audit_log\"");
        assertThat(updateText.parameters()).isEqualTo("Int32, Int32");
    }
}
