CREATE TABLE `PEACH_APPLICATION` (
  `APP_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用ID',
  `APP_NAME` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
  `APP_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用类型',
  `IS_OPEN` tinyint(1) DEFAULT '0' COMMENT '是否启用',
  `APP_DESC` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用描述',
  `LOGOUT_URL` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退出URL',
  `SORT_NUM` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '显示顺序',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  `IS_DELETE` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`APP_ID`),
  UNIQUE KEY `UK_APPLICATION_TENANT_APP` (`TENANT_ID`,`APP_ID`),
  KEY `IDX_APPLICATION_TENANT` (`TENANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
