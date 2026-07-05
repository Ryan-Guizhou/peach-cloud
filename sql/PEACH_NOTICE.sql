CREATE TABLE PEACH_NOTICE
(
    ID                  VARCHAR(64)  NOT NULL COMMENT '通知ID',
    NOTICE_CODE         VARCHAR(64)  NOT NULL COMMENT '通知编码',
    TITLE_MESSAGE_KEY   VARCHAR(128)     NULL COMMENT '标题消息Key',
    CONTENT_MESSAGE_KEY VARCHAR(128)     NULL COMMENT '内容消息Key',
    NOTICE_TYPE         VARCHAR(32)      NULL COMMENT '通知类型',
    PRIORITY            INT              NULL COMMENT '优先级',
    PUBLISH_STATUS      VARCHAR(32)      NULL COMMENT '发布状态',
    EFFECTIVE_FROM      VARCHAR(20)      NULL COMMENT '生效开始时间',
    EFFECTIVE_TO        VARCHAR(20)      NULL COMMENT '生效结束时间',
    READ_COUNT          INT              NULL COMMENT '阅读次数',
    INBOX_ENABLED       INT              NULL COMMENT '是否启用收件箱',
    STATUS              INT              NULL COMMENT '状态',
    CREATED_TIME        VARCHAR(20)      NULL COMMENT '创建时间',
    CREATOR_ID          VARCHAR(32)      NULL COMMENT '创建人ID',
    MODIFY_TIME         VARCHAR(20)      NULL COMMENT '修改时间',
    MODIFIER_ID         VARCHAR(32)      NULL COMMENT '修改人ID',
    PRIMARY KEY (ID)
) COMMENT='通知公告表';
