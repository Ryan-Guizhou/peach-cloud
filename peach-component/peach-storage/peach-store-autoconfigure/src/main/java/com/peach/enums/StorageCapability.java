package com.peach.enums;

/**
 * 存储增强能力枚举。
 *
 * <p>该枚举只描述不同 provider 之间的可选增强能力，不重复表达所有实现都必须具备的
 * 基础能力，例如上传、下载、删除和查询对象元信息。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public enum StorageCapability {

    /**
     * 支持生成前端直传令牌。
     */
    FRONTEND_UPLOAD_TOKEN,

    /**
     * 支持批量删除对象。
     */
    BATCH_DELETE,

    /**
     * 支持对象拷贝。
     */
    COPY,

    /**
     * 支持对象移动。
     */
    MOVE,

    /**
     * 支持判断 bucket 或根目录是否存在。
     */
    BUCKET_EXISTS,

    /**
     * 支持生成预签名 GET URL。
     */
    PRESIGNED_GET_URL,

    /**
     * 支持生成预签名 PUT URL。
     */
    PRESIGNED_PUT_URL,

    /**
     * 支持设置对象公共读 ACL。
     */
    PUBLIC_READ_ACL,

    /**
     * 支持自定义访问域名。
     */
    CUSTOM_DOMAIN,

    /**
     * 支持分片上传。
     */
    MULTIPART_UPLOAD,

    /**
     * 支持范围下载。
     */
    RANGE_DOWNLOAD
}