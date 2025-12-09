package com.peach.email.core;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 * @Description
 * 内嵌资源：在 HTML 中通过 CID 引用的图片或其他资源。
 */
public class InlineResource {

    /**
     * 内容ID（CID），用于在HTML中通过<img src="cid:...">引用
     */
    private final String contentId;

    /**
     * 内容类型（MIME），如 image/png
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

    public InlineResource(String contentId, String contentType, byte[] content, String path) {
        this.contentId = contentId;
        this.contentType = contentType;
        this.content = content != null ? content.clone() : null;
        this.path = path;
    }

    public String getContentId() {
        return contentId;
    }
    public String getContentType() {
        return contentType;
    }
    /** 返回二进制内容的防御性拷贝 */
    public byte[] getContent() {
        return content != null ? content.clone() : null;
    }
    public String getPath() {
        return path;
    }
}
