CREATE TABLE `PEACH_DICT_TYPE` (
  `ID` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `DICT_CODE` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典编码',
  `DICT_NAME` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '字典名称',
  `MODULE_CODE` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模块编码',
  `SORT_ORDER` int(11) DEFAULT NULL COMMENT '排序号',
  `STATUS` int(11) DEFAULT NULL COMMENT '状态',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_DICT_TYPE_CONTEXT_CODE` (`TENANT_ID`,`ORG_ID`,`DICT_CODE`),
  KEY `IDX_DICT_TYPE_CONTEXT` (`TENANT_ID`,`ORG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';
