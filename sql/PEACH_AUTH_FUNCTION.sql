CREATE TABLE `PEACH_AUTH_FUNCTION` (
  `ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '逻辑ID',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `PARTY_CODE` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与者代码',
  `PARTY_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与者类型，角色编码',
  `FUNC_CODE` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '功能代码',
  `FISCAL` smallint(4) NOT NULL COMMENT '年度',
  `STATE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '功能状态',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  `APP_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用ID',
  `IS_DELETE` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_AUTH_FUNCTION_CONTEXT` (`TENANT_ID`,`ORG_ID`,`APP_ID`,`PARTY_CODE`,`PARTY_TYPE`,`FISCAL`,`FUNC_CODE`),
  KEY `AUTH_FUNCTION_PF_IDX` (`TENANT_ID`,`ORG_ID`,`APP_ID`,`PARTY_CODE`,`FUNC_CODE`,`FISCAL`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='授权功能表';
