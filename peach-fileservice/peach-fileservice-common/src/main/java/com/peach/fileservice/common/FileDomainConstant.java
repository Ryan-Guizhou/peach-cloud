package com.peach.fileservice.common;


/**
 * 文件域常量定义。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description 文件域常量定义
 */
public final class FileDomainConstant {

    private FileDomainConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SYSTEM_OPERATOR = "system";
    public static final String DEFAULT_FILE_NAME = "upload.bin";
    public static final String DEFAULT_BIZ_TYPE = "common";
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    public static final String OBJECT_KEY_SEPARATOR = "/";
    public static final String OBJECT_KEY_DATE_PATTERN = "yyyyMMdd";
    public static final String BIZ_TYPE_ALLOWED_PATTERN = "[^a-zA-Z0-9/_-]";
    public static final String DIGEST_SHA256_ALGORITHM = "SHA-256";
    public static final String DIGEST_MD5_ALGORITHM = "MD5";
    public static final int BUFFER_SIZE = 8192;

    public static final class LogicDelete {

        private LogicDelete() {
        }

        public static final Integer NO = 0;
        public static final Integer YES = 1;
    }

    public static final class FileStatus {

        private FileStatus() {
        }

        public static final String UPLOAD_PENDING = "UPLOAD_PENDING";
        public static final String ACTIVE = "ACTIVE";
        public static final String DELETED = "DELETED";
        public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
    }

    public static final class StorageStatus {

        private StorageStatus() {
        }

        public static final String UPLOADING = "UPLOADING";
        public static final String ACTIVE = "ACTIVE";
        public static final String DELETE_PENDING = "DELETE_PENDING";
        public static final String DELETED = "DELETED";
        public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
    }

    public static final class SessionStatus {

        private SessionStatus() {
        }

        public static final String INITIATED = "INITIATED";
        public static final String UPLOADING = "UPLOADING";
        public static final String COMPLETED = "COMPLETED";
        public static final String ABORTED = "ABORTED";
        public static final String EXPIRED = "EXPIRED";
        public static final String FAILED = "FAILED";
    }
}
