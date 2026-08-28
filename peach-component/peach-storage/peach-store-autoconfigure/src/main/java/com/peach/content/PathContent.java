package com.peach.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 路径内容。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/10 15:26
 */
public class PathContent implements UploadContent {

    private final Path path;

    private final AtomicReference<InputStream> currentStream = new AtomicReference<>();

    public PathContent(Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path must be an existing file: " + path);
        }
        this.path = path;
    }

    @Override
    public InputStream read() throws IOException {
        InputStream stream = Files.newInputStream(path);
        currentStream.set(stream);
        return stream;
    }

    @Override
    public long length() throws IOException {
        return Files.size(path);
    }

    @Override
    public void close() throws IOException {
        InputStream stream = currentStream.getAndSet(null);
        if (stream != null) {
            stream.close();
        }
    }
}
