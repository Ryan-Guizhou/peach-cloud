package com.peach.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 基于字符串的上传内容。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/10 15:26
 */
public class StringContent implements UploadContent {

    private final byte[] buf;

    public StringContent(String text, Charset charset) {
        Charset actualCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        this.buf = (text == null ? "" : text).getBytes(actualCharset);
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
