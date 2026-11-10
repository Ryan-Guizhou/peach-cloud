package com.peach.satoken.dao;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证管理查询通过可关闭游标 SCAN 而非阻塞 KEYS，并遵守边界。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class PeachSaTokenScanTest {

    @Test
    @SuppressWarnings("unchecked")
    void scanSortsPagesAndClosesCursorWithoutKeysCommand() {
        PeachSaTokenDao dao = new PeachSaTokenDao(mock(RedisConnectionFactory.class), 5);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        Cursor<String> cursor = mock(Cursor.class);
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, true, false);
        when(cursor.next()).thenReturn("token:c", "token:a", "token:b");
        dao.stringRedisTemplate = template;

        assertThat(dao.searchData("token:", "", 1, 2, true))
                .containsExactly("token:b", "token:c");
        verify(template, never()).keys(anyString());
        verify(cursor).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void exceedingMatchingKeyBudgetFailsClosedAndClosesCursor() {
        PeachSaTokenDao dao = new PeachSaTokenDao(mock(RedisConnectionFactory.class), 2);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        Cursor<String> cursor = mock(Cursor.class);
        when(template.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, true);
        when(cursor.next()).thenReturn("token:1", "token:2", "token:3");
        dao.stringRedisTemplate = template;

        assertThatThrownBy(() -> dao.searchData("token:", "", 0, -1, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("limit exceeded");
        verify(cursor).close();
    }

    @Test
    void rejectsInvalidBudgetAndPaginationSize() {
        assertThatThrownBy(() -> new PeachSaTokenDao(mock(RedisConnectionFactory.class), 0))
                .isInstanceOf(IllegalArgumentException.class);
        PeachSaTokenDao dao = new PeachSaTokenDao(mock(RedisConnectionFactory.class), 10);
        assertThatThrownBy(() -> dao.searchData("token:", "", 0, -2, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(dao.searchData("token:", "", 0, 0, true)).isEqualTo(List.of());
    }
}
