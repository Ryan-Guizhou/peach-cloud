package com.peach.fileservice;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 10:45
 */
public enum StoreStrage {

    MINIO("MINIO","minio存储"),
    OSS("OSS","阿里云oss存储"),
    COS("COS","腾讯云cos存储"),
    CEPH("CEPH","ceph存储"),
    MONGO("MONGO","mongo存储"),
    AMAZON("AMAZON","亚马逊aws存储"),
    NAS("NAS","阿里云nas存储"),
    LOCAL("LOCAL","本地存储"),
    OBS("OBS","华为云obs存储");

    private String code;

    private String value;


    StoreStrage(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }
}
