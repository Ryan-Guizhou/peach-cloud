package com.peach.common.unique;

import com.peach.common.unique.impl.SnowflakeUniqueGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeIdGeneratorConfigTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty(UniqueGeneratorConst.PROPERTY_SNOWFLAKE_WORKER_ID);
        System.clearProperty(UniqueGeneratorConst.PROPERTY_SNOWFLAKE_DATACENTER_ID);
    }

    @Test
    void shouldRejectOutOfRangeConfiguredWorkerId() {
        System.setProperty(UniqueGeneratorConst.PROPERTY_SNOWFLAKE_WORKER_ID, "32");

        assertThatThrownBy(SnowflakeUniqueGenerator::new)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UniqueGeneratorConst.PROPERTY_SNOWFLAKE_WORKER_ID);
    }

    @Test
    void shouldRejectOutOfRangeConfiguredDatacenterId() {
        System.setProperty(UniqueGeneratorConst.PROPERTY_SNOWFLAKE_DATACENTER_ID, "-1");

        assertThatThrownBy(SnowflakeUniqueGenerator::new)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UniqueGeneratorConst.PROPERTY_SNOWFLAKE_DATACENTER_ID);
    }
}
