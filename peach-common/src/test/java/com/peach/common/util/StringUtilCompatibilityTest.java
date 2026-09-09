package com.peach.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilCompatibilityTest {

    @Test
    void shouldKeepLegacyHelpersAndHandleNullSafely() {
        assertThat(StringUtil.getUUID()).hasSize(32).doesNotContain("-");
        assertThat(StringUtil.toChinese(120)).isEqualTo("一百二十零");
        assertThat(StringUtil.lineToHumpOthersNoChange(null)).isEmpty();
        assertThat(StringUtil.humpToLine(null)).isEmpty();
    }
}
