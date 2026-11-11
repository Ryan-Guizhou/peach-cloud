package com.peach.storage.provider;

import com.peach.config.StorageProperties;
import com.peach.content.UploadContent;
import com.peach.enums.StorageType;
import com.peach.exception.StorageException;
import com.peach.request.UploadObjectRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证本地存储覆盖失败保护和符号链接根目录边界。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class LocalStorageSafetyTest {

    @TempDir
    Path root;

    @Test
    void failedUploadMustKeepPreviousObjectAndRemoveStagingFile() throws IOException {
        LocalFileStorageProvider provider = new LocalFileStorageProvider(config());
        provider.upload(UploadObjectRequest.builder()
                .objectKey("existing.txt")
                .content(UploadContent.of("original"))
                .build());

        UploadContent broken = new UploadContent() {
            @Override
            public InputStream read() {
                return new InputStream() {
                    private boolean first = true;

                    @Override
                    public int read() throws IOException {
                        if (first) {
                            first = false;
                            return 'x';
                        }
                        throw new IOException("Simulated upload read failure");
                    }
                };
            }
        };
        assertThrows(StorageException.class, () -> provider.upload(UploadObjectRequest.builder()
                .objectKey("existing.txt").content(broken).build()));

        assertEquals("original", Files.readString(root.resolve("tenant/existing.txt")));
        try (Stream<Path> children = Files.list(root.resolve("tenant"))) {
            assertFalse(children.anyMatch(p -> p.getFileName().toString().startsWith(".peach-upload-")));
        }
    }

    @Test
    void shouldRefuseSymbolicLinkWithinObjectPath() throws IOException {
        LocalFileStorageProvider provider = new LocalFileStorageProvider(config());
        Path external = Files.createTempDirectory("peach-external-");
        Path link = root.resolve("tenant/link");
        Files.createDirectories(link.getParent());
        try {
            try {
                Files.createSymbolicLink(link, external);
            } catch (UnsupportedOperationException | IOException | SecurityException ex) {
                org.junit.jupiter.api.Assumptions.assumeTrue(false, "Filesystem does not allow symlinks");
            }
            assertThrows(StorageException.class, () -> provider.upload(UploadObjectRequest.builder()
                    .objectKey("link/secret.txt")
                    .content(UploadContent.of("secret"))
                    .build()));
            assertFalse(Files.exists(external.resolve("secret.txt")));
            assertTrue(Files.isSymbolicLink(link));
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(external);
        }
    }

    private StorageProperties.StorageProvider config() {
        StorageProperties.StorageProvider c = new StorageProperties.StorageProvider();
        c.setType(StorageType.LOCAL);
        c.setName("local");
        c.setBucketName("bucket");
        c.setPrefix("tenant");
        c.setRootPath(root.toString());
        return c;
    }
}
