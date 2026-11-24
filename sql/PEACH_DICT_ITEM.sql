CREATE TABLE `PEACH_DICT_ITEM` (
  `ID` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `DICT_CODE` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '字典编码',
  `ITEM_CODE` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '项编码',
  `ITEM_VALUE` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '项值',
  `SORT_ORDER` int(11) DEFAULT NULL COMMENT '排序号',
  `MESSAGE_KEY` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国际化Key',
  `EXTRA_JSON` text COLLATE utf8mb4_unicode_ci COMMENT '扩展JSON',
  `STATUS` int(11) DEFAULT NULL COMMENT '状态',
  `TENANT_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
  `ORG_ID` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '机构ID',
  `CREATED_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建时间',
  `CREATOR_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人ID',
  `MODIFY_TIME` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改时间',
  `MODIFIER_ID` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人ID',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UK_DICT_ITEM_CONTEXT_CODE` (`TENANT_ID`,`ORG_ID`,`DICT_CODE`,`ITEM_CODE`),
  KEY `IDX_DICT_ITEM_CONTEXT_DICT` (`TENANT_ID`,`ORG_ID`,`DICT_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典项表';
