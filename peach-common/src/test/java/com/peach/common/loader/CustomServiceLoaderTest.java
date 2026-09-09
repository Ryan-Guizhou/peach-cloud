package com.peach.common.loader;

import com.peach.common.unique.UniqueGeneratorProvider;
import com.peach.common.unique.UniqueGenerator;
import com.peach.common.util.encrypt.EncryptProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CustomServiceLoader 单元测试。
 */
class CustomServiceLoaderTest {

    @AfterEach
    void tearDown() {
        CustomServiceLoader.clearCache();
    }

    @Test
    void shouldLoadStandardSpiProviders() {
        List<UniqueGeneratorProvider> providers = CustomServiceLoader.load(UniqueGeneratorProvider.class);
        assertNotNull(providers);
        assertFalse(providers.isEmpty());
        assertTrue(providers.stream().anyMatch(p -> "uuid".equalsIgnoreCase(p.type())));
        assertTrue(providers.stream().anyMatch(p -> "nanoid".equalsIgnoreCase(p.type())));
        assertTrue(providers.stream().anyMatch(p -> "snowflake".equalsIgnoreCase(p.type())));
    }

    @Test
    void shouldCacheLoadedProviders() {
        List<EncryptProvider> first = CustomServiceLoader.load(EncryptProvider.class);
        List<EncryptProvider> second = CustomServiceLoader.load(EncryptProvider.class);
        assertSame(first, second, "Repeated load should return cached instance list");

        List<EncryptProvider> reloaded = CustomServiceLoader.reload(EncryptProvider.class);
        assertNotNull(reloaded);
        assertEquals(first.size(), reloaded.size());
    }

    @Test
    void shouldSupportFindFirstAndLoadFirst() {
        Optional<UniqueGeneratorProvider> firstOpt = CustomServiceLoader.findFirst(UniqueGeneratorProvider.class);
        assertTrue(firstOpt.isPresent());

        UniqueGeneratorProvider defaultMock = new UniqueGeneratorProvider() {
            @Override
            public String type() {
                return "mock";
            }

            @Override
            public UniqueGenerator getGenerator() {
                return null;
            }
        };

        UniqueGeneratorProvider first = CustomServiceLoader.loadFirst(UniqueGeneratorProvider.class, defaultMock);
        assertNotNull(first);
        assertFalse("mock".equals(first.type()));

        // 未知接口返回 defaultProvider
        interface UnregisteredService {}
        UnregisteredService fallback = new UnregisteredService() {};
        UnregisteredService result = CustomServiceLoader.loadFirst(UnregisteredService.class, fallback);
        assertSame(fallback, result);
    }

    @Test
    void shouldLoadFromFileWithCommentsAndDeduplication() throws IOException {
        File tempFile = File.createTempFile("custom-spi-", ".txt");
        tempFile.deleteOnExit();

        String content = """
                # Top comment
                com.peach.common.unique.impl.provider.UuidUniqueGeneratorProvider # inline comment

                com.peach.common.unique.impl.provider.UuidUniqueGeneratorProvider
                # Non-existing class should not break others
                com.peach.common.nonexisting.DummyClass
                com.peach.common.unique.impl.provider.NanoUniqueProvider
                """;

        Files.writeString(tempFile.toPath(), content, StandardCharsets.UTF_8);

        List<UniqueGeneratorProvider> providers = CustomServiceLoader.loadFromFile(UniqueGeneratorProvider.class, tempFile);
        assertNotNull(providers);
        // UuidProvider 被写了两次，应该去重只保留一个；加上 NanoIdProvider 共 2 个
        assertEquals(2, providers.size());
        assertEquals("uuid", providers.get(0).type());
        assertEquals("nanoid", providers.get(1).type());
    }

    @Test
    void shouldValidateArguments() {
        assertThrows(NullPointerException.class, () -> CustomServiceLoader.load(null));
        assertThrows(NullPointerException.class, () -> CustomServiceLoader.loadFromFile(null, new File("dummy")));
        assertThrows(NullPointerException.class, () -> CustomServiceLoader.loadFromFile(UniqueGeneratorProvider.class, null));
    }
}
