CREATE TABLE `PEACH_NOTICE_READ_RECORD` (
  `ID` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `NOTICE_CODE` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知编码',
  `READ_USER_ID` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '阅读用户ID',
  `READ_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阅读时间',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_NOTICE_READ_CONTEXT` (`TENANT_ID`,`ORG_ID`,`NOTICE_CODE`,`READ_USER_ID`),
  KEY `IDX_NOTICE_READ_CONTEXT_USER` (`TENANT_ID`,`ORG_ID`,`READ_USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知阅读记录表';
