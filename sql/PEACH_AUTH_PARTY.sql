CREATE TABLE `PEACH_AUTH_PARTY` (
  `ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `ROLE_CODE` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色代码',
  `ROLE_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色类型',
  `PARTY_CODE` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与者代码',
  `PARTY_TYPE` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参与者类型',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  `IS_DELETE` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`TENANT_ID`,`ORG_ID`,`ROLE_CODE`,`ROLE_TYPE`,`PARTY_CODE`,`PARTY_TYPE`),
  KEY `AUTH_PARTY_BUSI_IDX` (`TENANT_ID`,`ORG_ID`,`ROLE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='授权参与者表';
