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

    /** 只保护生命周期状态和流注册，不在监视器内做文件 I/O。 */
    private final Object lifecycleLock = new Object();

    private boolean closed;

    public PathContent(Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path must be an existing file: " + path);
        }
        this.path = path;
    }

    @Override
    public InputStream read() throws IOException {
        synchronized (lifecycleLock) {
            if (closed) {
                throw new IOException("Upload content is already closed");
            }
        }
        // 文件打开可能阻塞，不能在 Java 21 的 monitor 内执行。
        InputStream stream = Files.newInputStream(path);
        synchronized (lifecycleLock) {
            if (!closed) {
                openedStreams.add(stream);
                return stream;
            }
        }
        // close 已赢得生命周期竞态，必须回收刚打开但未注册的流。
        stream.close();
        throw new IOException("Upload content is already closed");
    }

    @Override
    public long length() throws IOException {
        return Files.size(path);
    }

    @Override
    public void close() throws IOException {
        List<InputStream> toClose;
        synchronized (lifecycleLock) {
            if (closed) {
                return;
            }
            closed = true;
            toClose = new ArrayList<>(openedStreams);
            openedStreams.clear();
        }
        // 关闭文件流可能阻塞，状态已发布；其他线程读不到未注册的流。
        IOException failure = null;
        for (InputStream stream : toClose) {
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
        if (failure != null) {
            throw failure;
        }
    }
}
