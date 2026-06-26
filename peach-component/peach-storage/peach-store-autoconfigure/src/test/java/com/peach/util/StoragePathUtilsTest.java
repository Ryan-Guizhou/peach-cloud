package com.peach.util;

import com.peach.exception.StorageException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoragePathUtilsTest {

    @Test
    void shouldNormalizeObjectKey() {
        assertEquals("docs/a.txt", StoragePathUtil.normalizeObjectKey("\\docs//a.txt/"));
    }

    @Test
    void shouldRejectTraversalObjectKey() {
        assertThrows(StorageException.class, () -> StoragePathUtil.normalizeObjectKey("../secret.txt"));
    }

    @Test
    void shouldRejectResolvedPathOutsideRoot() {
        Path root = Paths.get("target/test-root").toAbsolutePath().normalize();
        assertThrows(StorageException.class, () -> StoragePathUtil.resolveLocalPath(root, "../secret.txt"));
    }
}
