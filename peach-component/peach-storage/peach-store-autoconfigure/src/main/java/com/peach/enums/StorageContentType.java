package com.peach.enums;

/**
 * 存储Content类型枚举。
 * <p>枚举覆盖业务中最常见的文本、图片、音视频、压缩包、Office 文档和二进制流场景。
 * 未覆盖的类型仍可以通过 {@code UploadObjectRequest.Builder#contentType(String)} 直接传入 MIME。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public enum StorageContentType {

    /**
     * 未知二进制流。
     */
    APPLICATION_OCTET_STREAM("application/octet-stream"),

    /**
     * 普通文本，UTF-8 编码。
     */
    TEXT_PLAIN_UTF8("text/plain;charset=UTF-8"),

    /**
     * HTML 文档，UTF-8 编码。
     */
    TEXT_HTML_UTF8("text/html;charset=UTF-8"),

    /**
     * JSON 文档，UTF-8 编码。
     */
    APPLICATION_JSON_UTF8("application/json;charset=UTF-8"),

    /**
     * XML 文档，UTF-8 编码。
     */
    APPLICATION_XML_UTF8("application/xml;charset=UTF-8"),

    /**
     * 表单提交。
     */
    FORM_URLENCODED("application/x-www-form-urlencoded"),

    /**
     * PNG 图片。
     */
    IMAGE_PNG("image/png"),

    /**
     * JPEG 图片。
     */
    IMAGE_JPEG("image/jpeg"),

    /**
     * GIF 图片。
     */
    IMAGE_GIF("image/gif"),

    /**
     * WebP 图片。
     */
    IMAGE_WEBP("image/webp"),

    /**
     * SVG 图片。
     */
    IMAGE_SVG("image/svg+xml"),

    /**
     * PDF 文档。
     */
    APPLICATION_PDF("application/pdf"),

    /**
     * ZIP 压缩包。
     */
    APPLICATION_ZIP("application/zip"),

    /**
     * GZIP 压缩包。
     */
    APPLICATION_GZIP("application/gzip"),

    /**
     * Excel xlsx 文档。
     */
    EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

    /**
     * Word docx 文档。
     */
    WORD_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),

    /**
     * PowerPoint pptx 文档。
     */
    POWERPOINT_PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation"),

    /**
     * MP4 视频。
     */
    VIDEO_MP4("video/mp4"),

    /**
     * MP3 音频。
     */
    AUDIO_MPEG("audio/mpeg");

    private final String value;

    StorageContentType(String value) {
        this.value = value;
    }

    /**
     * MIME 字符串。
     *
     * @return MIME 字符串
     */
    public String value() {
        return value;
    }
}
