CREATE TABLE `PEACH_ORGANIZATION` (
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_CODE` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构编码',
  `ORG_NAME` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构名称',
  `STATUS` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT 'ENABLE' COMMENT '状态',
  `SORT_NUM` int(11) DEFAULT '0' COMMENT '排序',
  `IS_DELETE` int(11) DEFAULT '0' COMMENT '逻辑删除',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`ORG_ID`),
  UNIQUE KEY `UK_ORG_CODE` (`TENANT_ID`,`ORG_CODE`),
  KEY `IDX_ORG_TENANT_ID` (`TENANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
