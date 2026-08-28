package com.peach.storage.constants;


/**
 * CloudStorageClassNames相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 18:02
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
     * AWSS3SDKv1。。
     */
    public static final String AWS_S3 = "com.amazonaws.services.s3.AmazonS3";

    /**
     * MinIO。。
     */
    public static final String MINIO = "io.minio.MinioClient";

}
