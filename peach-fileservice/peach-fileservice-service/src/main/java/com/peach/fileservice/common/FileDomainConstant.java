package com.peach.fileservice.common;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description 文件域常量定义
 */
public interface FileDomainConstant {

    String SYSTEM_OPERATOR = "system";

    interface LogicDelete {
        Integer NO = 0;
        Integer YES = 1;
    }

    interface FileStatus {
        String UPLOAD_PENDING = "UPLOAD_PENDING";
        String ACTIVE = "ACTIVE";
        String DELETED = "DELETED";
        String UPLOAD_FAILED = "UPLOAD_FAILED";
    }

    interface StorageStatus {
        String UPLOADING = "UPLOADING";
        String ACTIVE = "ACTIVE";
        String DELETE_PENDING = "DELETE_PENDING";
        String DELETED = "DELETED";
        String UPLOAD_FAILED = "UPLOAD_FAILED";
    }

    interface SessionStatus {
        String INITIATED = "INITIATED";
        String UPLOADING = "UPLOADING";
        String COMPLETED = "COMPLETED";
        String ABORTED = "ABORTED";
        String EXPIRED = "EXPIRED";
        String FAILED = "FAILED";
    }
}

