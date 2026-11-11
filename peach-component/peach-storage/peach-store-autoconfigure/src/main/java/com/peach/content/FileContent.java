package com.peach.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 可重复打开的本地文件上传内容。
 *
 * <p>每次读取返回独立文件流；上传完成后调用 {@link #close()} 会兜底关闭
 * 所有读取期间创建的流，避免重试覆盖上一条流引用。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/10 15:15
 */
public class FileContent implements UploadContent {

    private final File file;

    private final List<InputStream> openedStreams = new ArrayList<>();

    /** 只保护生命周期状态和流注册，不在监视器内做文件 I/O。 */
    private final Object lifecycleLock = new Object();

    private boolean closed;

    public FileContent(File file) throws FileNotFoundException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + file);
        }
        this.file = file;
    }

    @Override
    public InputStream read() throws IOException {
        synchronized (lifecycleLock) {
            if (closed) {
                throw new IOException("Upload content is already closed");
            }
        }
        // 文件打开可能阻塞，不能在 Java 21 的 monitor 内执行。
        InputStream stream;
        try {
            stream = new FileInputStream(file);
        } catch (IOException ex) {
            throw new IllegalStateException("File disappeared: " + file, ex);
        }
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
    public long length() {
        return file.length();
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
