package com.peach.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于输入流的上传内容。
 *
 * <p>输入流通常只能读取一次，适合直接转发请求体等场景。需要重试上传时，应优先使用
 * FileContent、PathContent 或 ByteArrayContent。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/10 15:46
 */
public class InputStreamContent implements UploadContent {

    private final InputStream in;

    /**
     * 内容长度，未知时为 -1。
     */
    private final long length;

    public InputStreamContent(InputStream in, long length) {
        this.in = in == null ? new ByteArrayInputStream(new byte[0]) : in;
        this.length = length;
    }

    @Override
    public InputStream read() {
        return in;
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
