-- #232: System-defined file API action directory.
-- Only permission DEFINITIONS are created; never grant to tenant users automatically.
-- Provision file row/field policies and active role bindings separately using Auth IAM.
-- This SQL requires the latest structured METHOD:/path matcher; legacy '*' API paths
-- are not valid authorization evidence. Check uniqueness before one-time deployment.
INSERT INTO PEACH_PERMISSION_DEFINITION
    (ID, PERMISSION_ID, TENANT_REF_ID, APP_REF_ID, PERMISSION_CODE,
     PERMISSION_NAME, RESOURCE_TYPE, OWNER_TYPE, DELEGABLE, STATUS,
     IS_DELETE, VERSION, CREATED_TIME, CREATOR_ID)
VALUES
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_LIST', NULL, NULL, 'POST:/api/file/internal/pageList',
     '查询文件列表', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_CHECK', NULL, NULL, 'POST:/api/file/internal/upload/check',
     '文件秒传预检', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_UPLOAD', NULL, NULL, 'POST:/api/file/internal/upload',
     '上传文件', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_VIEW', NULL, NULL, 'GET:/api/file/internal/{fileId}',
     '读取文件详情', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_URL', NULL, NULL, 'GET:/api/file/internal/{fileId}/url',
     '签名下载文件', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_DELETE', NULL, NULL, 'DELETE:/api/file/internal/{fileId}',
     '逻辑删除文件', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_RESTORE', NULL, NULL, 'POST:/api/file/internal/{fileId}/restore',
     '恢复文件', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_MP_INIT', NULL, NULL, 'POST:/api/file/internal/multipart/init',
     '分片初始化', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_MP_PART', NULL, NULL, 'POST:/api/file/internal/multipart/part-url',
     '获取分片预签名URL', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_MP_DONE', NULL, NULL, 'POST:/api/file/internal/multipart/complete',
     '完成分片上传', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_MP_ABORT', NULL, NULL, 'POST:/api/file/internal/multipart/abort/{sessionId}',
     '中止分片上传', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_EXT_UPLOAD', NULL, NULL, 'POST:/api/file/external/upload',
     '受信服务外部上传', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_EXT_READ', NULL, NULL, 'GET:/api/file/external/{fileId}',
     '受信服务外部读取', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_EXT_URL', NULL, NULL, 'GET:/api/file/external/{fileId}/url',
     '受信服务外部签名下载', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_EXT_DELETE', NULL, NULL, 'DELETE:/api/file/external/{fileId}',
     '受信服务外部删除', 'API', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM');

-- Independent DATA semantic permissions; no roles or row policies are granted.
-- Within the same role, each permission MUST have a PEACH_ROLE_DATA_POLICY row;
-- an API grant alone never unlocks the file object or field contents.
INSERT INTO PEACH_PERMISSION_DEFINITION
    (ID, PERMISSION_ID, TENANT_REF_ID, APP_REF_ID, PERMISSION_CODE,
     PERMISSION_NAME, RESOURCE_TYPE, OWNER_TYPE, DELEGABLE, STATUS,
     IS_DELETE, VERSION, CREATED_TIME, CREATOR_ID)
VALUES
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_DATA_LIST', NULL, NULL, 'FILE.LIST',
     '文件列表行级范围', 'DATA', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_DATA_READ', NULL, NULL, 'FILE.READ',
     '文件对象读取行级范围', 'DATA', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_DATA_DOWNLOAD', NULL, NULL, 'FILE.DOWNLOAD',
     '文件签名下载行级范围', 'DATA', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_DATA_UPLOAD', NULL, NULL, 'FILE.UPLOAD',
     '文件上传行级范围', 'DATA', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_DATA_DELETE', NULL, NULL, 'FILE.DELETE',
     '文件逻辑删除行级范围', 'DATA', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_DATA_RESTORE', NULL, NULL, 'FILE.RESTORE',
     '文件恢复行级范围', 'DATA', 'SYSTEM', 1, 'ACTIVE',
     0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM');

-- Auth still requires online Token + member/auth version and valid SYSTEM grants.
-- Disable old wildcard file API permissions before enabling the guarded file endpoints.

-- Readable fields for FILE_RECORD. Definitions only; no default READ_EFFECT/WRITE_EFFECT
-- or ROLE_FIELD_POLICY rows are created. Missing policy remains DENY.
INSERT INTO PEACH_FIELD_DEFINITION
    (ID, FIELD_ID, TENANT_REF_ID, APP_REF_ID, OWNER_TYPE, RESOURCE_CODE,
     FIELD_CODE, MASK_STRATEGY, DELEGABLE, STATUS, IS_DELETE, VERSION,
     CREATED_TIME, CREATOR_ID)
VALUES
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_FILEID', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'fileId', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_FILENAME', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'fileName', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_DISPLAYNAME', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'displayName', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_CONTENTTYPE', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'contentType', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_FILESIZE', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'fileSize', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_FILEEXT', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'fileExt', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_FILESTATUS', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'fileStatus', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_BIZTYPE', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'bizType', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_BIZID', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'bizId', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_BIZTAG', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'bizTag', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_REMARK', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'remark', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'FLD_FILE_CREATEDTIME', NULL, NULL,
     'SYSTEM', 'FILE_RECORD', 'createdTime', 'FULL', 1,
     'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM');

-- SQL view results, signed download URLs and multipart session metadata must not
-- include internal bucket/objectKey/provider, tenant or owner physical identifiers.
-- Use protected Auth policy management to grant individual fields as ALLOW or MASK.

-- #232: FILE.EDIT 元数据写权限。仅注册定义；不创建角色授予/字段写策略。
INSERT INTO PEACH_PERMISSION_DEFINITION
    (ID, PERMISSION_ID, TENANT_REF_ID, APP_REF_ID, PERMISSION_CODE,
     PERMISSION_NAME, RESOURCE_TYPE, OWNER_TYPE, DELEGABLE, STATUS,
     IS_DELETE, VERSION, CREATED_TIME, CREATOR_ID)
VALUES
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_METADATA_PATCH', NULL, NULL,
     'PATCH:/api/file/internal/{fileId}/metadata', '修改受控文件元数据 API',
     'API', 'SYSTEM', 1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_FILE_SAAS_DATA_EDIT', NULL, NULL,
     'FILE.EDIT', '受控文件元数据行级写权限',
     'DATA', 'SYSTEM', 1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM');
