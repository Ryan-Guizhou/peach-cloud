package com.peach.fileservice.common;

/**
 * 文件API常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public final class FileApiConstant {

    public static final String INTERNAL_PREFIX = "/file/internal";
    public static final String INTERNAL_TOOLS_PREFIX = INTERNAL_PREFIX + "/tools";
    public static final String INTERNAL_STORAGE_PREFIX = INTERNAL_PREFIX + "/storage";
    public static final String INTERNAL_STORAGE_INSTANCE_PREFIX = INTERNAL_STORAGE_PREFIX + "/instance";
    public static final String INTERNAL_STORAGE_BROWSER_PREFIX = INTERNAL_STORAGE_PREFIX + "/browser";

    public static final String EXTERNAL_PREFIX = "/file/external";
    public static final String EXTERNAL_UPLOAD = "/upload";
    public static final String EXTERNAL_SHA256 = "/tools/sha256";
    public static final String EXTERNAL_FILE_ID = "/{fileId}";
    public static final String EXTERNAL_FILE_URL = "/{fileId}/url";

    private FileApiConstant() {
    }
}
