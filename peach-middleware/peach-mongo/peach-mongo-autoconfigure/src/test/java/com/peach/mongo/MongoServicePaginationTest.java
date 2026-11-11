package com.peach.mongo;

import com.github.pagehelper.PageInfo;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mongo 分页总数和错误传播的最小回归验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 11:30
 */
class MongoServicePaginationTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldPreserveLongDocumentTotal() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> iterable = mock(FindIterable.class);
        when(template.getCollection("audit")).thenReturn(collection);
        when(collection.countDocuments(any(Document.class))).thenReturn(3_000_000_000L);
        when(collection.find(any(Document.class))).thenReturn(iterable);
        when(iterable.skip(anyInt())).thenReturn(iterable);
        when(iterable.limit(anyInt())).thenReturn(iterable);
        doAnswer(invocation -> invocation.getArgument(0))
                .when(iterable).into(any(ArrayList.class));

        PageInfo<Document> page = new MongoService<Document>(template)
                .findPage("audit", 1, 100);

        assertThat(page.getTotal()).isEqualTo(3_000_000_000L);
        assertThat(page.getPages()).isEqualTo(30_000_000);
    }

    @Test
    void shouldRejectInvalidPagingBeforeDatabaseAccess() {
        MongoService<Document> service = new MongoService<>(mock(MongoTemplate.class));

        assertThatThrownBy(() -> service.findPage("audit", 0, 50))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findPage("audit", Integer.MAX_VALUE, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPropagateMongoReadFailure() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(template.getCollection("audit")).thenReturn(collection);
        when(collection.find(any(Document.class))).thenThrow(new IllegalStateException("unavailable"));

        assertThatThrownBy(() -> new MongoService<Document>(template).findList("audit"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mongo list query failed");
    }
}
