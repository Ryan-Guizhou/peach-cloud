CREATE TABLE `PEACH_AUTH_LOG` (
  `LOG_ID` varchar(38) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '日志ID',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `OPERATOR_USER_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人用户ID',
  `OPERATOR_CODE` varchar(42) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人账号',
  `OPERATOR_NAME` varchar(42) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人名称',
  `USER_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '被授权用户ID',
  `USER_CODE` varchar(42) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '被授权用户账号',
  `USER_NAME` varchar(42) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '被授权用户名称',
  `AUTH_DESCRIBE` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '授权描述',
  `OPERAT_TIME` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`LOG_ID`),
  KEY `IDX_AUTH_LOG_TENANT_ORG_TIME` (`TENANT_ID`,`ORG_ID`,`OPERAT_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='授权日志表';
