package com.peach.email.core;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 * @Description
 * 附件模型：支持二进制内容或文件路径；可设置文件名、内容类型与处置方式。
 */
public class Attachment {
    /**
     * 文件名（用于邮件客户端展示），选填
     */
    private final String filename;

    /**
     * 内容类型（MIME），选填，缺省为 application/octet-stream
     */
    private final String contentType;

    /**
     * 二进制内容（与 path 二选一）；内部保存为防御性拷贝
     */
    private final byte[] content;

    /**
     * 文件路径（与 content 二选一）；当提供路径时将读取文件数据
     */
    private final String path;

    /**
     * 处置方式：attachment 或 inline，选填
     */
    private final String disposition;


    public Attachment(String filename, String contentType, byte[] content, String path, String disposition) {
        this.filename = filename;
        this.contentType = contentType;
        this.content = content != null ? content.clone() : null;
        this.path = path;
        this.disposition = disposition;
    }


    /**
     * 使用数据源的构造函数（流式读取），适合大附件
     */
    public Attachment(String filename, String contentType, String disposition) {
        this.filename = filename;
        this.contentType = contentType;
        this.content = null;
        this.path = null;
        this.disposition = disposition;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content != null ? content.clone() : null;
    }

    public String getPath() {
        return path;
    }

    public String getDisposition() {
        return disposition;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Attachment{");
        sb.append("filename='").append(filename).append('\'');
        sb.append(", contentType='").append(contentType).append('\'');
        sb.append(", content=").append(content);
        sb.append(", path='").append(path).append('\'');
        sb.append(", disposition='").append(disposition).append('\'');
        sb.append('}');
        return sb.toString();
    }

    /**
     * 附件类型：内部嵌套或附件
     */
    public static enum AttachmentType{
        INLINE("inline","内部嵌套"),
        ATTACHMENT("attachment","附件");

        private String type;
        private String desc;

        AttachmentType(String type, String desc){
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
