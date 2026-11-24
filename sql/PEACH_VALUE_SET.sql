CREATE TABLE `PEACH_VALUE_SET` (
  `ID` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `VALUE_SET_CODE` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '值集编码',
  `VALUE_SET_NAME` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '值集名称',
  `MODULE_CODE` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模块编码',
  `SOURCE_TYPE` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源类型',
  `DESCRIPTION` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `STATUS` int(11) DEFAULT NULL COMMENT '状态',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_VALUE_SET_CONTEXT_CODE` (`TENANT_ID`,`ORG_ID`,`VALUE_SET_CODE`),
  KEY `IDX_VALUE_SET_CONTEXT` (`TENANT_ID`,`ORG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='值集配置表';
