package com.peach.email.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Inline 资源值对象。
 *
 * @param contentId   内容标识符（Content-ID），用于 HTML 邮件内联引用
 * @param contentType 资源的 MIME 类型（如 image/png）
 * @param content     资源的二进制内容
 * @param path        资源路径
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 */
public record InlineResource(String contentId, String contentType, byte[] content, String path) {

    public InlineResource {
        content = content != null ? Arrays.copyOf(content, content.length) : null;
    }

    /**
     * 返回二进制内容的防御性拷贝。
     *
     * @return 内容字节数组的拷贝；若内容为 null 则返回 null
     */
    public byte[] content() {
        return content != null ? Arrays.copyOf(content, content.length) : null;
    }

}
