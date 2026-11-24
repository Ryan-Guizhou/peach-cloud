CREATE TABLE `PEACH_USER_ORG` (
  `ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `USER_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户ID',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `IS_DEFAULT` tinyint(1) DEFAULT '0' COMMENT '是否默认机构',
  `STATUS` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  `IS_DELETE` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_USER_ORG` (`TENANT_ID`,`USER_ID`,`ORG_ID`),
  KEY `IDX_USER_ORG_USER` (`TENANT_ID`,`USER_ID`),
  KEY `IDX_USER_ORG_ORG` (`TENANT_ID`,`ORG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户机构关系表';
