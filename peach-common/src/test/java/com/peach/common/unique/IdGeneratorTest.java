package com.peach.common.unique;

import com.peach.common.unique.impl.SnowflakeUniqueGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ID 生成器与 SPI 扩展行为测试。
 */
class IdGeneratorTest {

    @Test
    void shouldGenerateValidUuidWithoutHyphen() {
        String uuid = UniqueIdFacade.generateUuid();
        assertNotNull(uuid);
        assertEquals(32, uuid.length());
        assertFalse(uuid.contains("-"));
        assertTrue(uuid.matches("^[0-9a-fA-F]{32}$"));

        String uuidViaNextId = UniqueIdFacade.nextId(UniqueGeneratorConst.UUID);
        assertEquals(32, uuidViaNextId.length());
        assertFalse(uuidViaNextId.contains("-"));
    }

    @Test
    void shouldGenerateValidNanoId() {
        String nanoId = UniqueIdFacade.generateNanoId();
        assertNotNull(nanoId);
        assertEquals(21, nanoId.length());
        assertTrue(nanoId.matches("^[0-9a-zA-Z_-]{21}$"));

        String nanoIdViaNextId = UniqueIdFacade.nextId(UniqueGeneratorConst.NANOID);
        assertEquals(21, nanoIdViaNextId.length());
        assertTrue(nanoIdViaNextId.matches("^[0-9a-zA-Z_-]{21}$"));
    }

    @Test
    void shouldGenerateValidSnowflakeId() {
        String snowflakeIdStr = UniqueIdFacade.generateSnowflakeId();
        assertNotNull(snowflakeIdStr);
        assertTrue(snowflakeIdStr.matches("^\\d+$"));

        long longId1 = UniqueIdFacade.nextLongId();
        long longId2 = UniqueIdFacade.nextLongId();
        assertTrue(longId1 > 0);
        assertTrue(longId2 >= longId1);
    }

    @Test
    void shouldFallbackGracefullyToDefaultGenerator() {
        // 验证默认生成器可用（默认兜底为 UUID）
        String defaultId = UniqueIdFacade.nextId();
        assertNotNull(defaultId);
        assertFalse(defaultId.isBlank());

        // 验证空参数自动路由到默认生成器
        String defaultViaBlank = UniqueIdFacade.nextId("");
        assertNotNull(defaultViaBlank);
        assertEquals(defaultId.length(), defaultViaBlank.length());
    }

    @Test
    void shouldSupportManualProviderRegistration() {
        UniqueGenerator customGenerator = new UniqueGenerator() {
            @Override
            public String nextId() {
                return "custom-id-12345";
            }

            @Override
            public String type() {
                return "mock_custom";
            }
        };

        UniqueGeneratorRegistry.register(new UniqueGeneratorProvider() {
            @Override
            public String type() {
                return "mock_custom";
            }

            @Override
            public UniqueGenerator getGenerator() {
                return customGenerator;
            }
        });

        assertEquals("custom-id-12345", UniqueIdFacade.nextId("mock_custom"));
    }

    @Test
    void shouldValidateSnowflakeConstructorBounds() {
        // 有效边界
        SnowflakeUniqueGenerator gen = new SnowflakeUniqueGenerator(0, 0);
        assertEquals(0, gen.getWorkerId());
        assertEquals(0, gen.getDatacenterId());

        SnowflakeUniqueGenerator genMax = new SnowflakeUniqueGenerator(31, 31);
        assertEquals(31, genMax.getWorkerId());
        assertEquals(31, genMax.getDatacenterId());

        // 超界拒绝
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeUniqueGenerator(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeUniqueGenerator(32, 0));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeUniqueGenerator(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeUniqueGenerator(0, 32));
    }

    @Test
    void shouldGenerateUniqueIdsUnderConcurrency() {
        int count = 1000;
        Set<String> nanoIds = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            nanoIds.add(UniqueIdFacade.generateNanoId());
        }
        assertEquals(count, nanoIds.size());

        Set<String> uuids = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(UniqueIdFacade.generateUuid());
        }
        assertEquals(count, uuids.size());

        Set<Long> snowflakes = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            snowflakes.add(UniqueIdFacade.nextLongId());
        }
        assertEquals(count, snowflakes.size());
    }

    @Test
    void shouldThrowWhenProviderNotFound() {
        assertThrows(IllegalArgumentException.class, () -> UniqueIdFacade.nextId("unknown_provider"));
    }

    @Test
    void shouldThrowWhenNanoIdRequestsNumericId() {
        UniqueGenerator nanoGenerator = UniqueIdFacade.getGenerator(UniqueGeneratorConst.NANOID);
        assertThrows(UnsupportedOperationException.class, nanoGenerator::nextLongId);
    }
}
