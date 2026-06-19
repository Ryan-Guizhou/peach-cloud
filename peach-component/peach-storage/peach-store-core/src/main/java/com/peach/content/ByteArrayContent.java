package com.peach.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 基于字节数组的上传内容。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/10 14:54
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
