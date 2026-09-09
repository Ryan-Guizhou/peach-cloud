package com.peach.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void shouldHoldPagedResultAndTotals() {
        PageResult<String> pageResult = new PageResult<>(List.of("a", "b"), 10L)
                .setTopTotal(2L);

        assertThat(pageResult.getResult()).containsExactly("a", "b");
        assertThat(pageResult.getTotal()).isEqualTo(10L);
        assertThat(pageResult.getTopTotal()).isEqualTo(2L);
    }

    @Test
    void shouldSlicePageListByIndexAndSize() {
        PageList<Integer> pageList = new PageList<Integer>()
                .setPageIndex(2)
                .setPageSize(2);

        assertThat(pageList.getPageList(List.of(1, 2, 3, 4, 5))).containsExactly(3, 4);
    }
}