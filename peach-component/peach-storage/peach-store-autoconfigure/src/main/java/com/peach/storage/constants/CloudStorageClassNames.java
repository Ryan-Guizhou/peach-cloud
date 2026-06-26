package com.peach.storage.constants;


/**
 * 云存储 SDK 核心类全限定名。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 18:02
 */
public final class CloudStorageClassNames {


    private CloudStorageClassNames() {
    }

    /**
     * 阿里云 OSS
     */
    public static final String ALIYUN_OSS = "com.aliyun.oss.OSS";

    /**
     * 百度云 BOS
     */
    public static final String BAIDU_BOS = "com.baidubce.services.bos.BosClient";

    /**
     * 华为云 OBS
     */
    public static final String HUAWEI_OBS = "com.obs.services.ObsClient";

    /**
     * 腾讯云 COS
     */
    public static final String TENCENT_COS = "com.qcloud.cos.COSClient";

    /**
     * AWS S3 SDK v1
     */
    public static final String AWS_S3 = "com.amazonaws.services.s3.AmazonS3";

    /**
     * MinIO
     */
    public static final String MINIO = "io.minio.MinioClient";

}
