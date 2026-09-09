package com.peach.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PeachCollectionUtilTest {

    @Test
    void shouldCheckCollectionAndMapEmptiness() {
        assertThat(PeachCollectionUtil.isEmpty((List<?>) null)).isTrue();
        assertThat(PeachCollectionUtil.isEmpty(List.of())).isTrue();
        assertThat(PeachCollectionUtil.isNotEmpty(List.of("x"))).isTrue();

        assertThat(PeachCollectionUtil.isEmpty((Map<?, ?>) null)).isTrue();
        assertThat(PeachCollectionUtil.isEmpty(Map.of())).isTrue();
        assertThat(PeachCollectionUtil.isNotEmpty(Map.of("k", "v"))).isTrue();
    }

    @Test
    void shouldCollectAllNonEmptyLists() {
        assertThat(PeachCollectionUtil.collectAll(List.of("a"), List.of(), List.of("b", "c")))
                .containsExactly("a", "b", "c");
    }

    @Test
    void shouldPartitionList() {
        assertThat(PeachCollectionUtil.partition(List.of(1, 2, 3), 2))
                .containsExactly(List.of(1, 2), List.of(3));
    }
}