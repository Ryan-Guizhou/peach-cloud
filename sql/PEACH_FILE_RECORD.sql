CREATE TABLE PEACH_FILE_RECORD
(
    FILE_ID           VARCHAR(64)  NOT  NULL COMMENT '业务文件ID',
    OBJECT_ID         VARCHAR(64)       NULL COMMENT '物理对象ID',
    BIZ_TYPE          VARCHAR(32)       NULL COMMENT '业务类型',
    BIZ_ID            VARCHAR(64)       NULL COMMENT '业务ID',
    BIZ_TAG           VARCHAR(64)       NULL COMMENT '业务标签',
    FILE_NAME         VARCHAR(255)      NULL COMMENT '文件名',
    DISPLAY_NAME      VARCHAR(255)      NULL COMMENT '显示文件名',
    CONTENT_TYPE      VARCHAR(128)      NULL COMMENT '内容类型',
    FILE_SIZE         BIGINT            NULL COMMENT '文件大小',
    FILE_EXT          VARCHAR(32)       NULL COMMENT '扩展名',
    FILE_STATUS       VARCHAR(32)       NULL COMMENT '文件状态',
    DELETE_TIME       VARCHAR(20)       NULL COMMENT '逻辑删除时间',
    EXPIRE_DELETE_TIME VARCHAR(20)      NULL COMMENT '过期删除时间',
    REMARK            VARCHAR(500)      NULL COMMENT '备注',
    IS_DELETE         TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除标记',
    CREATED_TIME      VARCHAR(20)       NULL COMMENT '创建时间',
    CREATOR_ID        VARCHAR(32)       NULL COMMENT '创建人ID',
    MODIFY_TIME       VARCHAR(20)       NULL COMMENT '修改时间',
    MODIFIER_ID       VARCHAR(32)       NULL COMMENT '修改人ID',
    PRIMARY KEY (FILE_ID)
) COMMENT='业务文件记录表';
