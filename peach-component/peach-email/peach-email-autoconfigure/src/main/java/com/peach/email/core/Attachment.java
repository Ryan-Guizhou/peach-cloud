package com.peach.email.core;

import java.util.Arrays;

/**
 * Attachment 值对象，表示邮件附件。
 *
 * @param filename    附件文件名（不含路径）
 * @param contentType 附件的 MIME 类型（如 "image/png"），可为 null
 * @param content     附件二进制内容的字节数组（内部会进行防御性拷贝）
 * @param path        附件来源路径（若从文件系统读取），可为 null
 * @param disposition 内容处置方式，通常为 "inline" 或 "attachment"，对应 {@link AttachmentType}
 *
 * @Author MrShu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 */
public record Attachment(
        String filename,
        String contentType,
        byte[] content,
        String path,
        String disposition) {

    /**
     * 紧凑构造器：对 content 进行防御性拷贝，避免外部修改影响内部状态。
     */
    public Attachment {
        content = content != null ? Arrays.copyOf(content, content.length) : null;
    }

    /**
     * 使用数据源（流式读取）的构造方法，适合大附件场景。
     * 此时 {@code content} 和 {@code path} 均为 null，需通过其他方式提供数据。
     *
     * @param filename    附件文件名
     * @param contentType MIME 类型
     * @param disposition 处置方式（如 "attachment"）
     */
    public Attachment(String filename, String contentType, String disposition) {
        this(filename, contentType, null, null, disposition);
    }

    /**
     * 获取二进制内容的防御性拷贝副本。
     *
     * @return 内容的副本，若原内容为 null 则返回 null
     */
    @Override
    public byte[] content() {
        return content != null ? Arrays.copyOf(content, content.length) : null;
    }

    /**
     * 附件类型枚举，对应 RFC 2183 中的 Content-Disposition 值。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    public enum AttachmentType {
        /** 内嵌资源（如邮件中的图片） */
        INLINE("inline", "内部嵌套"),
        /** 普通附件 */
        ATTACHMENT("attachment", "附件");

        private final String type;
        private final String desc;

        AttachmentType(String type, String desc) {
            this.type = type;
            this.desc = desc;
        }

        public String getType() {
            return type;
        }

        public String getDesc() {
            return desc;
        }
    }
}
