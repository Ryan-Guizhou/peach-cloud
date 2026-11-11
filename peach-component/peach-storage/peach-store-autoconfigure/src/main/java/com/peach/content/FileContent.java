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

    private boolean closed;

    public FileContent(File file) throws FileNotFoundException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + file);
        }
        this.file = file;
    }

    @Override
    public synchronized InputStream read() throws IOException {
        if (closed) {
            throw new IOException("Upload content is already closed");
        }
        try {
            InputStream stream = new FileInputStream(file);
            openedStreams.add(stream);
            return stream;
        } catch (IOException ex) {
            throw new IllegalStateException("File disappeared: " + file, ex);
        }
    }

    @Override
    public long length() {
        return file.length();
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
