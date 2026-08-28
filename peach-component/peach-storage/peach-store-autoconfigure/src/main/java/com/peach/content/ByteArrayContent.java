package com.peach.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 字节数组内容。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/10 14:54
 */
public class ByteArrayContent implements UploadContent {

    private final byte[] buf;

    public ByteArrayContent(byte[] buf) {
        this.buf = buf == null ? new byte[0] : buf;
    }

    @Override
    public InputStream read() {
        return new ByteArrayInputStream(buf);
    }

    @Override
    public long length() {
        return buf.length;
    }
}
