package com.peach.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 可重复打开的本地路径上传内容。
 *
 * <p>每次读取都会创建独立输入流。调用方应关闭返回的流；上传完成后调用
 * {@link #close()} 可兜底关闭所有已打开流，避免重试时覆盖流引用并泄漏文件句柄。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/10 15:26
 */
public class PathContent implements UploadContent {

    private final Path path;

    private final List<InputStream> openedStreams = new ArrayList<>();

    private boolean closed;

    public PathContent(Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path must be an existing file: " + path);
        }
        this.path = path;
    }

    @Override
    public synchronized InputStream read() throws IOException {
        if (closed) {
            throw new IOException("Upload content is already closed");
        }
        InputStream stream = Files.newInputStream(path);
        openedStreams.add(stream);
        return stream;
    }

    @Override
    public long length() throws IOException {
        return Files.size(path);
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        for (InputStream stream : openedStreams) {
            try {
                stream.close();
            } catch (IOException ex) {
                if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        openedStreams.clear();
        if (failure != null) {
            throw failure;
        }
    }
}
