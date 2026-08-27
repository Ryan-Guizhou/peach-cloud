package com.peach.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于本地文件的上传内容。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/10 15:15
 */
public class FileContent implements UploadContent {

    private final File file;

    private final AtomicReference<InputStream> currentStream = new AtomicReference<>();

    public FileContent(File file) throws FileNotFoundException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + file);
        }
        this.file = file;
    }

    @Override
    public InputStream read() throws IOException {
        try {
            InputStream stream = new FileInputStream(file);
            currentStream.set(stream);
            return stream;
        } catch (IOException e) {
            throw new IllegalStateException("File disappeared: " + file, e);
        }
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public void close() throws IOException {
        InputStream stream = currentStream.getAndSet(null);
        if (stream != null) {
            stream.close();
        }
    }
}
