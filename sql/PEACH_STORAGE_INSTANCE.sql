CREATE TABLE PEACH_STORAGE_INSTANCE
(
    INSTANCE_ID          VARCHAR(64)  NOT NULL COMMENT '存储实例ID',
    INSTANCE_NAME        VARCHAR(128) NOT NULL COMMENT '存储实例名称',
    STORE_TYPE           VARCHAR(32)  NOT NULL COMMENT '存储类型',
    ENDPOINT             VARCHAR(512) DEFAULT NULL COMMENT '访问节点地址',
    REGION               VARCHAR(64)  DEFAULT NULL COMMENT '区域',
    BUCKET_NAME          VARCHAR(128) DEFAULT NULL COMMENT '存储桶名称',
    PREFIX               VARCHAR(512) DEFAULT NULL COMMENT '逻辑路径前缀',
    ACCESS_KEY           VARCHAR(256) DEFAULT NULL COMMENT '访问密钥',
    SECRET_KEY           VARCHAR(512) DEFAULT NULL COMMENT '加密后的私密密钥',
    ROOT_PATH            VARCHAR(512) DEFAULT NULL COMMENT '本地存储或NAS存储根路径',
    DOMAIN               VARCHAR(512) DEFAULT NULL COMMENT '公共访问域名',
    PATH_STYLE_ACCESS    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否启用Path Style访问，0否1是',
    PUBLIC_READ          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否允许公共读取，0否1是',
    EXTRA_JSON           JSON         DEFAULT NULL COMMENT '服务商扩展配置JSON',
    ENABLED              TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用，0禁用1启用',
    BUILT_IN             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否内置，0否1是',
    REMARK               VARCHAR(512) DEFAULT NULL COMMENT '备注',
    CREATED_TIME         VARCHAR(20)  DEFAULT NULL COMMENT '创建时间',
    CREATOR_ID           VARCHAR(32)  DEFAULT NULL COMMENT '创建人ID',
    MODIFY_TIME          VARCHAR(20)  DEFAULT NULL COMMENT '修改时间',
    MODIFIER_ID          VARCHAR(32)  DEFAULT NULL COMMENT '修改人ID',
    PRIMARY KEY (INSTANCE_ID),
    UNIQUE KEY UK_STORAGE_INSTANCE_NAME (INSTANCE_NAME),
    KEY IDX_STORAGE_INSTANCE_TYPE (STORE_TYPE),
    KEY IDX_STORAGE_INSTANCE_ENABLE (ENABLED),
    KEY IDX_STORAGE_INSTANCE_TYPE_ENABLE (STORE_TYPE, ENABLED)
) COMMENT='云存储实例定义表';


INSERT INTO PEACH_STORAGE_INSTANCE (INSTANCE_ID, INSTANCE_NAME, STORE_TYPE, ENDPOINT, REGION, BUCKET_NAME, PREFIX,
                                    ACCESS_KEY, SECRET_KEY, ROOT_PATH, DOMAIN, PATH_STYLE_ACCESS, PUBLIC_READ, EXTRA_JSON, ENABLED, BUILT_IN, REMARK,
                                    CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES ('oss-default-instance', '阿里云OSS默认存储', 'OSS', 'https://oss-cn-hangzhou.aliyuncs.com',
        'cn-hangzhou', 'peach-file-prod', 'peach/', 'your-access-key', 'encrypted-secret-key',
        NULL, 'https://peach-file-prod.oss-cn-hangzhou.aliyuncs.com', 0, 0,
        '{"endpointType":"OSS","storageClass":"STANDARD","multipartUpload":true,"transferAcceleration":false}',
        1, 1, '系统默认OSS存储实例', '2026-06-19 10:00:00', 'system', '2026-06-19 10:00:00', 'system');
