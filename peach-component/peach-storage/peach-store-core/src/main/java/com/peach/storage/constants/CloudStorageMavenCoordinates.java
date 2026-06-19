package com.peach.storage.constants;


/**
 * 常见云存储 SDK Maven 坐标。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 18:02
 */
public final class CloudStorageMavenCoordinates {

    private CloudStorageMavenCoordinates() {
    }

    /**
     * 阿里云 OSS
     */
    public static final String ALIYUN_OSS = "com.aliyun.oss:aliyun-sdk-oss";

    /**
     * 华为云 OBS
     */
    public static final String HUAWEI_OBS = "com.huaweicloud:esdk-obs-java";

    /**
     * 腾讯云 COS
     */
    public static final String TENCENT_COS = "com.qcloud:cos_api";

    /**
     * AWS S3 SDK v1
     */
    public static final String AWS_S3 = "com.amazonaws:aws-java-sdk-s3";

    /**
     * MinIO
     */
    public static final String MINIO = "io.minio:minio";

    /**
     * 百度云 BOS
     */
    public static final String BAIDU_BOS = "com.baidubce:bce-java-sdk";

}