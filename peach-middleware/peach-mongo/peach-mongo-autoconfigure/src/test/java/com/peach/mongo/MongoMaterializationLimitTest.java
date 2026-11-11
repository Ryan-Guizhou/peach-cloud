package com.peach.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证无界 Mongo 查询超过限制时显式失败且不静默丢失结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class MongoMaterializationLimitTest {

    @Test
    @SuppressWarnings("unchecked")
    void unboundedListShouldFailWhenMoreDocumentsExistThanAllowed() {
        PeachMongoProperties properties = new PeachMongoProperties();
        properties.setMaxListSize(2);
        MongoTemplate template = mock(MongoTemplate.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(template.getCollection(anyString())).thenReturn(collection);
        when(collection.find(any(Document.class))).thenReturn(iterable);
        when(iterable.limit(3)).thenReturn(iterable);
        doAnswer(invocation -> {
            ArrayList<Document> results = invocation.getArgument(0);
            results.add(new Document("id", 1));
            results.add(new Document("id", 2));
            results.add(new Document("id", 3));
            return results;
        }).when(iterable).into(any(ArrayList.class));

        MongoService<Document> service = new MongoService<>(template, properties);
        assertThatThrownBy(() -> service.findList("audit"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-list-size");
    }

    @Test
    void explicitOversizedLimitOrPageMustFailWithoutRunningMongoQuery() {
        PeachMongoProperties properties = new PeachMongoProperties();
        properties.setMaxListSize(2);
        properties.setMaxPageSize(3);
        MongoService<Document> service = new MongoService<>(mock(MongoTemplate.class), properties);

        assertThatThrownBy(() -> service.findList("audit", null, null, null, 0, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findPage("audit", 1, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
