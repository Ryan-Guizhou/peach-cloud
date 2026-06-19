package com.peach.content;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 基于 {@link Path} 的上传内容。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/10 15:26
 */
public class PathContent implements UploadContent {

    private final Path path;

    private volatile InputStream currentStream;

    public PathContent(Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path must be an existing file: " + path);
        }
        this.path = path;
    }

    @Override
    public InputStream read() throws Exception {
        currentStream = Files.newInputStream(path);
        return currentStream;
    }

    @Override
    public long length() throws Exception {
        return Files.size(path);
    }

    @Override
    public void close() throws Exception {
        if (currentStream != null) {
            try {
                currentStream.close();
            } finally {
                currentStream = null;
            }
        }
    }
}
