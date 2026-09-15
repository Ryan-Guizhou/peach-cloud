-- Initial data is intentionally scoped to one tenant and one application.
-- All primary-key values are 32-character UUIDs without hyphens.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @TENANT_ID = '0a8e3d5f7c4b4e3a9d2c1b0f8e6a5c4d';
SET @DEFAULT_ORG_ID = '1b9f4e6a8d3c4f2b9a7e5d1c0b8f6a4e';
SET @BRANCH_ORG_ID = '2c8e5a7d9f4b4c1e8a6d3b0f7e5c2a9d';
SET @ADMIN_USER_ID = '3d7f6a5c8e4b4d1a9c2e0f7b6a5d3c8e';
SET @APP_ID = 'f73b300578a5436d82ec7fca2c07c284';
SET @NOW = '2026-04-05 00:00:00';



INSERT IGNORE INTO PEACH_TENANT
    (TENANT_ID, TENANT_CODE, TENANT_NAME, STATUS, SORT_NUM, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    (@TENANT_ID, 'DEFAULT', '默认租户', 'ENABLE', 1, 0, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_APPLICATION
    (APP_ID, APP_NAME, APP_TYPE, IS_OPEN, APP_DESC, LOGOUT_URL, SORT_NUM, TENANT_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE)
VALUES
    (@APP_ID, '管理平台', 'SYSTEM', 1, '租户、机构和权限管理平台', NULL, '1', @TENANT_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0);

INSERT IGNORE INTO PEACH_ORGANIZATION
    (ORG_ID, TENANT_ID, ORG_CODE, ORG_NAME, STATUS, SORT_NUM, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    (@DEFAULT_ORG_ID, @TENANT_ID, 'DEFAULT', '默认机构', 'ENABLE', 1, 0, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    (@BRANCH_ORG_ID, @TENANT_ID, 'BRANCH', '示例分支机构', 'ENABLE', 2, 0, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_USER
    (USER_ID, USER_CODE, PASSWORD, USER_NAME, AUTH_MODE, STATUS, MENU_STYLE, MENU_ROLE, START_DATE, END_DATE, MOBILE_PHONE, EMAIL, TENANT_ID, DEFAULT_ORG_ID, IS_DELETE, IS_MODIFY, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    (@ADMIN_USER_ID, 'admin', 'MTIzNDU2', '系统管理员', 'PASSWORD', '1', 'LEFT', 'ROLE_SYS_ADMIN', '2026-01-01', '2099-12-31', '13800000001', 'admin@peach.com', @TENANT_ID, @DEFAULT_ORG_ID, 0, 1, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_USER_ORG
    (ID, USER_ID, TENANT_ID, ORG_ID, IS_DEFAULT, STATUS, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE)
VALUES
    ('4e6a8d3c1b9f4e2a8c5d7f0b6a3e9c1d', @ADMIN_USER_ID, @TENANT_ID, @DEFAULT_ORG_ID, 1, 'ENABLE', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('5f7b9e4d2c8a4f1b9d6e3a0c7b5f2d8e', @ADMIN_USER_ID, @TENANT_ID, @BRANCH_ORG_ID, 0, 'ENABLE', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0);

INSERT IGNORE INTO PEACH_ROLE
    (ROLE_ID, TENANT_ID, ORG_ID, ROLE_CODE, FISCAL, ROLE_NAME, ROLE_DESC, ROLE_SCOPE, ROLE_TYPE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE, SKIP_URL)
VALUES
    ('6a5d3c8e1f7b4d2a9e0c6b5f3a8d1e4c', @TENANT_ID, @DEFAULT_ORG_ID, 'ROLE_SYS_ADMIN', 2026, '系统管理员', '默认机构的系统管理角色', 'ORG', 'SYSTEM', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, '/user'),
    ('7b6e4d9f2a8c4e1b9d5f3a0c6b7e2d8a', @TENANT_ID, @BRANCH_ORG_ID, 'ROLE_SYS_ADMIN', 2026, '分支机构管理员', '分支机构的系统管理角色', 'ORG', 'SYSTEM', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, '/user');

INSERT IGNORE INTO PEACH_AUTH_PARTY
    (ID, TENANT_ID, ORG_ID, ROLE_CODE, ROLE_TYPE, FISCAL, PARTY_CODE, PARTY_TYPE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE)
VALUES
    ('8c7f5e1a3b9d4c2e8a6f0b7d5c3e1a9f', @TENANT_ID, @DEFAULT_ORG_ID, 'ROLE_SYS_ADMIN', 'SYSTEM', 2026, 'admin', 'USER', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('9d8a6f2b4c1e4d3a8b7f0c6e5d2a9b1f', @TENANT_ID, @BRANCH_ORG_ID, 'ROLE_SYS_ADMIN', 'SYSTEM', 2026, 'admin', 'USER', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0);

INSERT IGNORE INTO PEACH_FUNCTION
    (FUNC_ID, FUNC_CODE, PARENT_FUNC_CODE, FUNC_NAME, FUNC_DESC, FUNC_URL, FUNC_SEQ, FUNC_TYPE, IS_MENU, IS_AUTHORIZE, TENANT_ID, APP_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DISABLE, IS_DELETE)
VALUES
    ('10101010101040108080808080808080', 'SYS_SETTING', NULL, '系统设置', '系统设置根功能', '/system-setting', '001', 'CATALOG', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('10202020202040209090909090909090', 'SYS_WORKSPACE', 'SYS_SETTING', '工作台', '系统概览工作台', '/workspace', '001001', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1030303030304030a0a0a0a0a0a0a0a0', 'SYS_USER_MGMT', 'SYS_SETTING', '用户管理', '用户及所属机构维护', '/user', '001002', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1040404040404040b0b0b0b0b0b0b0b0', 'SYS_ROLE_MGMT', 'SYS_SETTING', '角色管理', '机构角色维护', '/role', '001003', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1050505050504050c0c0c0c0c0c0c0c0', 'SYS_MENU_MGMT', 'SYS_SETTING', '菜单管理', '菜单定义维护', '/menu', '001004', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1060606060604060d0d0d0d0d0d0d0d0', 'SYS_FUNC_MGMT', 'SYS_SETTING', '功能管理', '功能定义维护', '/function', '001005', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1070707070704070e0e0e0e0e0e0e0e0', 'SYS_ROUTER_MGMT', 'SYS_SETTING', '路由管理', '前端路由维护', '/router', '001006', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1080808080804080f0f0f0f0f0f0f0f0', 'SYS_ORG_MGMT', 'SYS_SETTING', '机构管理', '组织机构维护', '/organization', '001007', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1090909090904090a0a0a0a0a0a0a0a0', 'SYS_RESOURCE_MGMT', 'SYS_SETTING', '资源管理', '按钮和接口资源维护', '/resource', '001008', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('10a0a0a0a0a040a0b0b0b0b0b0b0b0b0', 'SYS_AUTH_MGMT', 'SYS_SETTING', '角色授权', '角色、菜单和资源授权', '/authorization', '001009', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('10b0b0b0b0b040b0c0c0c0c0c0c0c0c0', 'SYS_NOTICE_MGMT', 'SYS_SETTING', '公告管理', '系统公告维护', '/notice', '001010', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('10c0c0c0c0c040c0d0d0d0d0d0d0d0d0', 'SYS_MESSAGE_CENTER', 'SYS_SETTING', '消息中心', '站内消息和待办处理', '/message-center', '001011', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('10d0d0d0d0d040d0e0e0e0e0e0e0e0e0', 'SYS_DICT_MGMT', 'SYS_SETTING', '字典管理', '数据字典维护', '/dict', '001012', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('10e0e0e0e0e040e0f0f0f0f0f0f0f0f0', 'SYS_VALUE_SET_MGMT', 'SYS_SETTING', '值集管理', '值集和值集项维护', '/value-set', '001013', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('10f0f0f0f0f040f0a1a1a1a1a1a1a1a1', 'SYS_MULTI_MESSAGE_MGMT', 'SYS_SETTING', '多语言管理', '多语言文案维护', '/multi-message', '001014', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1101010101014010b1b1b1b1b1b1b1b1', 'SYS_IP_WHITELIST_MGMT', 'SYS_SETTING', 'IP 白名单', '网关 IP 白名单维护', '/ip-whitelist', '001015', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1111111111114111c1c1c1c1c1c1c1c1', 'SYS_FILE_RECORD', 'SYS_SETTING', '文件管理', '业务文件记录查询', '/file-record', '001016', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1121212121214121d1d1d1d1d1d1d1d1', 'SYS_STORAGE_INSTANCE', 'SYS_SETTING', '存储实例', '云存储实例维护', '/storage-instance', '001017', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1131313131314131e1e1e1e1e1e1e1e1', 'SYS_OBJECT_BROWSER', 'SYS_SETTING', '对象浏览', '存储对象浏览和目录维护', '/object-browser', '001018', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1141414141414141f1f1f1f1f1f1f1f1', 'SYS_RUNTIME_MONITOR', 'SYS_SETTING', '运行监控', '运行时监控快照', '/runtime-monitor', '001019', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1151515151514151a2a2a2a2a2a2a2a2', 'SYS_AUTH_LOG', 'SYS_SETTING', '授权日志', '授权操作日志查询', '/auth-log', '001020', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1161616161614161b2b2b2b2b2b2b2b2', 'SYS_OPER_LOG', 'SYS_SETTING', '操作日志', '用户操作日志查询', '/operation-log', '001021', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0),
    ('1171717171714171c2c2c2c2c2c2c2c2', 'SYS_PROFILE', 'SYS_SETTING', '个人中心', '个人资料和头像维护', '/profile', '001022', 'MENU', 1, 1, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0);

INSERT IGNORE INTO PEACH_MENU
    (MENU_ID, MENU_NAME, MENU_CODE, IS_LEAF, MENU_URL, MENU_PARAM, PARENT_MENU_ID, MENU_LEVEL, SORT_NO, COLLAPSE_ICON, EXPAND_ICON, MENU_SEQ, OPEN_MODE, SUBCOUNT, FUNC_CODE, MENU_APP_ID, TENANT_ID, APP_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE, IS_DISABLE, IS_SHOW, SF_BLANK, MENU_ICON)
VALUES
    ('20101010101040108080808080808080', '系统设置', 'MENU_SYS_SETTING', 0, '/workspace', NULL, NULL, '1', '1', NULL, NULL, '001', 'SELF', '22', 'SYS_SETTING', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Setting'),
    ('20202020202040209090909090909090', '工作台', 'workspace', 1, '/workspace', NULL, '20101010101040108080808080808080', '2', '1', NULL, NULL, '001001', 'SELF', '0', 'SYS_WORKSPACE', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Dashboard'),
    ('2030303030304030a0a0a0a0a0a0a0a0', '用户管理', 'user', 1, '/user', NULL, '20101010101040108080808080808080', '2', '2', NULL, NULL, '001002', 'SELF', '0', 'SYS_USER_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'User'),
    ('2040404040404040b0b0b0b0b0b0b0b0', '角色管理', 'role', 1, '/role', NULL, '20101010101040108080808080808080', '2', '3', NULL, NULL, '001003', 'SELF', '0', 'SYS_ROLE_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'UserSwitch'),
    ('2050505050504050c0c0c0c0c0c0c0c0', '菜单管理', 'menu', 1, '/menu', NULL, '20101010101040108080808080808080', '2', '4', NULL, NULL, '001004', 'SELF', '0', 'SYS_MENU_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Menu'),
    ('2060606060604060d0d0d0d0d0d0d0d0', '功能管理', 'function', 1, '/function', NULL, '20101010101040108080808080808080', '2', '5', NULL, NULL, '001005', 'SELF', '0', 'SYS_FUNC_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Grid'),
    ('2070707070704070e0e0e0e0e0e0e0e0', '路由管理', 'router', 1, '/router', NULL, '20101010101040108080808080808080', '2', '6', NULL, NULL, '001006', 'SELF', '0', 'SYS_ROUTER_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Link'),
    ('2080808080804080f0f0f0f0f0f0f0f0', '机构管理', 'organization', 1, '/organization', NULL, '20101010101040108080808080808080', '2', '7', NULL, NULL, '001007', 'SELF', '0', 'SYS_ORG_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Cluster'),
    ('2090909090904090a0a0a0a0a0a0a0a0', '资源管理', 'resource', 1, '/resource', NULL, '20101010101040108080808080808080', '2', '8', NULL, NULL, '001008', 'SELF', '0', 'SYS_RESOURCE_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Pointer'),
    ('20a0a0a0a0a040a0b0b0b0b0b0b0b0b0', '角色授权', 'authorization', 1, '/authorization', NULL, '20101010101040108080808080808080', '2', '9', NULL, NULL, '001009', 'SELF', '0', 'SYS_AUTH_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'SafetyCertificate'),
    ('20b0b0b0b0b040b0c0c0c0c0c0c0c0c0', '公告管理', 'notice', 1, '/notice', NULL, '20101010101040108080808080808080', '2', '10', NULL, NULL, '001010', 'SELF', '0', 'SYS_NOTICE_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Notification'),
    ('20c0c0c0c0c040c0d0d0d0d0d0d0d0d0', '消息中心', 'messageCenter', 1, '/message-center', NULL, '20101010101040108080808080808080', '2', '11', NULL, NULL, '001011', 'SELF', '0', 'SYS_MESSAGE_CENTER', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Message'),
    ('20d0d0d0d0d040d0e0e0e0e0e0e0e0e0', '字典管理', 'dict', 1, '/dict', NULL, '20101010101040108080808080808080', '2', '12', NULL, NULL, '001012', 'SELF', '0', 'SYS_DICT_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Book'),
    ('20e0e0e0e0e040e0f0f0f0f0f0f0f0f0', '值集管理', 'valueSet', 1, '/value-set', NULL, '20101010101040108080808080808080', '2', '13', NULL, NULL, '001013', 'SELF', '0', 'SYS_VALUE_SET_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Database'),
    ('20f0f0f0f0f040f0a1a1a1a1a1a1a1a1', '多语言管理', 'multiMessage', 1, '/multi-message', NULL, '20101010101040108080808080808080', '2', '14', NULL, NULL, '001014', 'SELF', '0', 'SYS_MULTI_MESSAGE_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Translation'),
    ('2101010101014010b1b1b1b1b1b1b1b1', 'IP 白名单', 'ipWhitelist', 1, '/ip-whitelist', NULL, '20101010101040108080808080808080', '2', '15', NULL, NULL, '001015', 'SELF', '0', 'SYS_IP_WHITELIST_MGMT', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Global'),
    ('2111111111114111c1c1c1c1c1c1c1c1', '文件管理', 'fileRecord', 1, '/file-record', NULL, '20101010101040108080808080808080', '2', '16', NULL, NULL, '001016', 'SELF', '0', 'SYS_FILE_RECORD', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'File'),
    ('2121212121214121d1d1d1d1d1d1d1d1', '存储实例', 'storageInstance', 1, '/storage-instance', NULL, '20101010101040108080808080808080', '2', '17', NULL, NULL, '001017', 'SELF', '0', 'SYS_STORAGE_INSTANCE', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'CloudServer'),
    ('2131313131314131e1e1e1e1e1e1e1e1', '对象浏览', 'objectBrowser', 1, '/object-browser', NULL, '20101010101040108080808080808080', '2', '18', NULL, NULL, '001018', 'SELF', '0', 'SYS_OBJECT_BROWSER', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'FolderOpen'),
    ('2141414141414141f1f1f1f1f1f1f1f1', '运行监控', 'runtimeMonitor', 1, '/runtime-monitor', NULL, '20101010101040108080808080808080', '2', '19', NULL, NULL, '001019', 'SELF', '0', 'SYS_RUNTIME_MONITOR', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Monitor'),
    ('2151515151514151a2a2a2a2a2a2a2a2', '授权日志', 'authLog', 1, '/auth-log', NULL, '20101010101040108080808080808080', '2', '20', NULL, NULL, '001020', 'SELF', '0', 'SYS_AUTH_LOG', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Audit'),
    ('2161616161614161b2b2b2b2b2b2b2b2', '操作日志', 'operationLog', 1, '/operation-log', NULL, '20101010101040108080808080808080', '2', '21', NULL, NULL, '001021', 'SELF', '0', 'SYS_OPER_LOG', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 1, 0, 'Profile'),
    ('2171717171714171c2c2c2c2c2c2c2c2', '个人中心', 'profile', 1, '/profile', NULL, '20101010101040108080808080808080', '2', '22', NULL, NULL, '001022', 'SELF', '0', 'SYS_PROFILE', @APP_ID, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 0, 0, 0, 'Idcard');

INSERT IGNORE INTO PEACH_ROUTER
    (ROUTER_ID, ROUTER_CODE, ROUTER_NAME, ROUTER_URL, FILE_PATH, IS_AUTH, IS_CACHE, MODULE_CODE, ROUTER_LEVEL, TENANT_ID, APP_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    ('30101010101040108080808080808080', 'SYS_WORKSPACE', '工作台', '/workspace', 'workspace/overview', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('30202020202040209090909090909090', 'SYS_USER_MGMT', '用户管理', '/user', 'system/user', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3030303030304030a0a0a0a0a0a0a0a0', 'SYS_ROLE_MGMT', '角色管理', '/role', 'system/role', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3040404040404040b0b0b0b0b0b0b0b0', 'SYS_MENU_MGMT', '菜单管理', '/menu', 'system/menu', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3050505050504050c0c0c0c0c0c0c0c0', 'SYS_FUNC_MGMT', '功能管理', '/function', 'system/function', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3060606060604060d0d0d0d0d0d0d0d0', 'SYS_ROUTER_MGMT', '路由管理', '/router', 'system/router', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3070707070704070e0e0e0e0e0e0e0e0', 'SYS_ORG_MGMT', '机构管理', '/organization', 'system/organization', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3080808080804080f0f0f0f0f0f0f0f0', 'SYS_RESOURCE_MGMT', '资源管理', '/resource', 'system/resource', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3090909090904090a0a0a0a0a0a0a0a0', 'SYS_AUTH_MGMT', '角色授权', '/authorization', 'system/authorization', 1, 0, 'system', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('30a0a0a0a0a040a0b0b0b0b0b0b0b0b0', 'SYS_NOTICE_MGMT', '公告管理', '/notice', 'setting/notice', 1, 0, 'setting', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('30b0b0b0b0b040b0c0c0c0c0c0c0c0c0', 'SYS_MESSAGE_CENTER', '消息中心', '/message-center', 'message/center', 1, 0, 'message', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('30c0c0c0c0c040c0d0d0d0d0d0d0d0d0', 'SYS_DICT_MGMT', '字典管理', '/dict', 'setting/dict', 1, 0, 'setting', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('30d0d0d0d0d040d0e0e0e0e0e0e0e0e0', 'SYS_VALUE_SET_MGMT', '值集管理', '/value-set', 'setting/value-set', 1, 0, 'setting', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('30e0e0e0e0e040e0f0f0f0f0f0f0f0f0', 'SYS_MULTI_MESSAGE_MGMT', '多语言管理', '/multi-message', 'setting/multi-message', 1, 0, 'setting', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('30f0f0f0f0f040f0a1a1a1a1a1a1a1a1', 'SYS_IP_WHITELIST_MGMT', 'IP 白名单', '/ip-whitelist', 'setting/ip-whitelist', 1, 0, 'setting', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3101010101014010b1b1b1b1b1b1b1b1', 'SYS_FILE_RECORD', '文件管理', '/file-record', 'file/record', 1, 0, 'file', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3111111111114111c1c1c1c1c1c1c1c1', 'SYS_STORAGE_INSTANCE', '存储实例', '/storage-instance', 'file/storage-instance', 1, 0, 'file', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3121212121214121d1d1d1d1d1d1d1d1', 'SYS_OBJECT_BROWSER', '对象浏览', '/object-browser', 'file/object-browser', 1, 0, 'file', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3131313131314131e1e1e1e1e1e1e1e1', 'SYS_RUNTIME_MONITOR', '运行监控', '/runtime-monitor', 'monitor/runtime', 1, 0, 'monitor', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3141414141414141f1f1f1f1f1f1f1f1', 'SYS_AUTH_LOG', '授权日志', '/auth-log', 'log/auth', 1, 0, 'log', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3151515151514151a2a2a2a2a2a2a2a2', 'SYS_OPER_LOG', '操作日志', '/operation-log', 'log/operation', 1, 0, 'log', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('3161616161614161b2b2b2b2b2b2b2b2', 'SYS_PROFILE', '个人中心', '/profile', 'user/profile', 1, 0, 'user', 2, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_RESOURCE
    (RESOURCE_ID, FUNC_CODE, RESOURCE_TYPE, RESOURCE_CODE, RESOURCE_NAME, TENANT_ID, APP_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE)
VALUES
    ('40101010101040108080808080808080', 'SYS_USER_MGMT', 'BUTTON', 'user:view', '查看用户', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('40202020202040209090909090909090', 'SYS_USER_MGMT', 'BUTTON', 'user:add', '新增用户', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4030303030304030a0a0a0a0a0a0a0a0', 'SYS_USER_MGMT', 'BUTTON', 'user:update', '修改用户', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4040404040404040b0b0b0b0b0b0b0b0', 'SYS_USER_MGMT', 'BUTTON', 'user:delete', '删除用户', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4050505050504050c0c0c0c0c0c0c0c0', 'SYS_ROLE_MGMT', 'BUTTON', 'role:view', '查看角色', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4060606060604060d0d0d0d0d0d0d0d0', 'SYS_ROLE_MGMT', 'BUTTON', 'role:add', '新增角色', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4070707070704070e0e0e0e0e0e0e0e0', 'SYS_ROLE_MGMT', 'BUTTON', 'role:update', '修改角色', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4080808080804080f0f0f0f0f0f0f0f0', 'SYS_ROLE_MGMT', 'BUTTON', 'role:delete', '删除角色', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4090909090904090a0a0a0a0a0a0a0a0', 'SYS_MENU_MGMT', 'BUTTON', 'menu:view', '查看菜单', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('40a0a0a0a0a040a0b0b0b0b0b0b0b0b0', 'SYS_MENU_MGMT', 'BUTTON', 'menu:add', '新增菜单', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('40b0b0b0b0b040b0c0c0c0c0c0c0c0c0', 'SYS_MENU_MGMT', 'BUTTON', 'menu:update', '修改菜单', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('40c0c0c0c0c040c0d0d0d0d0d0d0d0d0', 'SYS_MENU_MGMT', 'BUTTON', 'menu:delete', '删除菜单', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('40d0d0d0d0d040d0e0e0e0e0e0e0e0e0', 'SYS_ROUTER_MGMT', 'BUTTON', 'router:view', '查看路由', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('40e0e0e0e0e040e0f0f0f0f0f0f0f0f0', 'SYS_ROUTER_MGMT', 'BUTTON', 'router:add', '新增路由', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('40f0f0f0f0f040f0a1a1a1a1a1a1a1a1', 'SYS_ROUTER_MGMT', 'BUTTON', 'router:update', '修改路由', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4101010101014010b1b1b1b1b1b1b1b1', 'SYS_ROUTER_MGMT', 'BUTTON', 'router:delete', '删除路由', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4111111111114111c1c1c1c1c1c1c1c1', 'SYS_ORG_MGMT', 'BUTTON', 'organization:view', '查看机构', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4121212121214121d1d1d1d1d1d1d1d1', 'SYS_ORG_MGMT', 'BUTTON', 'organization:add', '新增机构', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4131313131314131e1e1e1e1e1e1e1e1', 'SYS_ORG_MGMT', 'BUTTON', 'organization:update', '修改机构', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0),
    ('4141414141414141f1f1f1f1f1f1f1f1', 'SYS_ORG_MGMT', 'BUTTON', 'organization:delete', '删除机构', @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0);

INSERT IGNORE INTO PEACH_RESOURCE
    (RESOURCE_ID, FUNC_CODE, RESOURCE_TYPE, RESOURCE_CODE, RESOURCE_NAME, TENANT_ID, APP_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE)
SELECT REPLACE(UUID(), '-', ''), FUNC_CODE, 'BUTTON', CONCAT(PERMISSION_PREFIX, ':view'), CONCAT(FUNC_NAME, '查看'), @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0
FROM (
    SELECT 'SYS_FUNC_MGMT' FUNC_CODE, 'function' PERMISSION_PREFIX, '功能' FUNC_NAME UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'resource', '资源' UNION ALL
    SELECT 'SYS_AUTH_MGMT', 'authorization', '授权' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'notice', '公告' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'messageCenter', '消息' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'dict', '字典' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'valueSet', '值集' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'multiMessage', '多语言' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'ipWhitelist', 'IP 白名单' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'fileRecord', '文件' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'storageInstance', '存储实例' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'objectBrowser', '对象浏览' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'runtimeMonitor', '运行监控' UNION ALL
    SELECT 'SYS_AUTH_LOG', 'authLog', '授权日志' UNION ALL
    SELECT 'SYS_OPER_LOG', 'operationLog', '操作日志'
) PERMISSION_SOURCE
UNION ALL
SELECT REPLACE(UUID(), '-', ''), FUNC_CODE, 'BUTTON', CONCAT(PERMISSION_PREFIX, ':add'), CONCAT(FUNC_NAME, '新增'), @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0
FROM (
    SELECT 'SYS_FUNC_MGMT' FUNC_CODE, 'function' PERMISSION_PREFIX, '功能' FUNC_NAME UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'resource', '资源' UNION ALL
    SELECT 'SYS_AUTH_MGMT', 'authorization', '授权' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'notice', '公告' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'dict', '字典' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'valueSet', '值集' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'multiMessage', '多语言' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'ipWhitelist', 'IP 白名单' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'storageInstance', '存储实例'
) PERMISSION_SOURCE
UNION ALL
SELECT REPLACE(UUID(), '-', ''), FUNC_CODE, 'BUTTON', CONCAT(PERMISSION_PREFIX, ':update'), CONCAT(FUNC_NAME, '修改'), @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0
FROM (
    SELECT 'SYS_FUNC_MGMT' FUNC_CODE, 'function' PERMISSION_PREFIX, '功能' FUNC_NAME UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'resource', '资源' UNION ALL
    SELECT 'SYS_AUTH_MGMT', 'authorization', '授权' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'notice', '公告' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'dict', '字典' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'valueSet', '值集' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'multiMessage', '多语言' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'ipWhitelist', 'IP 白名单' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'storageInstance', '存储实例'
) PERMISSION_SOURCE
UNION ALL
SELECT REPLACE(UUID(), '-', ''), FUNC_CODE, 'BUTTON', CONCAT(PERMISSION_PREFIX, ':delete'), CONCAT(FUNC_NAME, '删除'), @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0
FROM (
    SELECT 'SYS_FUNC_MGMT' FUNC_CODE, 'function' PERMISSION_PREFIX, '功能' FUNC_NAME UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'resource', '资源' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'notice', '公告' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'dict', '字典' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'valueSet', '值集' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'multiMessage', '多语言' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'ipWhitelist', 'IP 白名单' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'storageInstance', '存储实例'
) PERMISSION_SOURCE;

INSERT IGNORE INTO PEACH_RESOURCE
    (RESOURCE_ID, FUNC_CODE, RESOURCE_TYPE, RESOURCE_CODE, RESOURCE_NAME, TENANT_ID, APP_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE)
SELECT REPLACE(UUID(), '-', ''), FUNC_CODE, 'API', RESOURCE_CODE, RESOURCE_NAME, @TENANT_ID, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0
FROM (
    SELECT 'SYS_AUTH_MGMT' FUNC_CODE, 'POST:/api/auth/authFunction/pageList' RESOURCE_CODE, '分页查询角色功能授权' RESOURCE_NAME UNION ALL
    SELECT 'SYS_AUTH_MGMT', 'POST:/api/auth/authFunction/list', '查询角色功能授权列表' UNION ALL
    SELECT 'SYS_AUTH_MGMT', 'POST:/api/auth/authFunction/saveRoleFunctions', '保存角色功能授权' UNION ALL
    SELECT 'SYS_AUTH_MGMT', 'POST:/api/auth/authResource/pageList', '分页查询角色资源授权' UNION ALL
    SELECT 'SYS_AUTH_MGMT', 'POST:/api/auth/authResource/list', '查询角色资源授权列表' UNION ALL
    SELECT 'SYS_AUTH_MGMT', 'POST:/api/auth/authResource/saveRoleResources', '保存角色资源授权' UNION ALL
    SELECT 'SYS_AUTH_LOG', 'POST:/api/auth/authLog/pageList', '分页查询授权日志' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/switchContext', '切换登录机构上下文' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/changePassword', '修改当前用户密码' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/changeInfo', '修改当前用户信息' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/changeAvatar', '修改当前用户头像' UNION ALL
    SELECT 'SYS_PROFILE', 'GET:/api/auth/profile', '查询个人资料' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/profile/basic', '修改个人基础资料' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/profile/avatar', '上传个人头像' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/profile/avatar/*/select', '切换历史头像' UNION ALL
    SELECT 'SYS_USER_MGMT', 'POST:/api/auth/user/pageList', '分页查询用户' UNION ALL
    SELECT 'SYS_USER_MGMT', 'GET:/api/auth/user/selectById', '查询用户详情' UNION ALL
    SELECT 'SYS_USER_MGMT', 'POST:/api/auth/user/add', '新增用户' UNION ALL
    SELECT 'SYS_USER_MGMT', 'DELETE:/api/auth/user/delById', '删除用户' UNION ALL
    SELECT 'SYS_USER_MGMT', 'POST:/api/auth/user/update', '修改用户' UNION ALL
    SELECT 'SYS_ROLE_MGMT', 'POST:/api/auth/role/pageList', '分页查询角色' UNION ALL
    SELECT 'SYS_ROLE_MGMT', 'GET:/api/auth/role/selectById', '查询角色详情' UNION ALL
    SELECT 'SYS_ROLE_MGMT', 'POST:/api/auth/role/add', '新增角色' UNION ALL
    SELECT 'SYS_ROLE_MGMT', 'DELETE:/api/auth/role/delById', '删除角色' UNION ALL
    SELECT 'SYS_ROLE_MGMT', 'POST:/api/auth/role/update', '修改角色' UNION ALL
    SELECT 'SYS_MENU_MGMT', 'POST:/api/auth/menu/pageList', '分页查询菜单' UNION ALL
    SELECT 'SYS_MENU_MGMT', 'GET:/api/auth/menu/selectById', '查询菜单详情' UNION ALL
    SELECT 'SYS_MENU_MGMT', 'POST:/api/auth/menu/add', '新增菜单' UNION ALL
    SELECT 'SYS_MENU_MGMT', 'DELETE:/api/auth/menu/delById', '删除菜单' UNION ALL
    SELECT 'SYS_MENU_MGMT', 'POST:/api/auth/menu/update', '修改菜单' UNION ALL
    SELECT 'SYS_FUNC_MGMT', 'POST:/api/auth/function/pageList', '分页查询功能' UNION ALL
    SELECT 'SYS_FUNC_MGMT', 'GET:/api/auth/function/selectById', '查询功能详情' UNION ALL
    SELECT 'SYS_FUNC_MGMT', 'POST:/api/auth/function/add', '新增功能' UNION ALL
    SELECT 'SYS_FUNC_MGMT', 'DELETE:/api/auth/function/delById', '删除功能' UNION ALL
    SELECT 'SYS_FUNC_MGMT', 'POST:/api/auth/function/update', '修改功能' UNION ALL
    SELECT 'SYS_ROUTER_MGMT', 'POST:/api/auth/router/pageList', '分页查询路由' UNION ALL
    SELECT 'SYS_ROUTER_MGMT', 'GET:/api/auth/router/selectById', '查询路由详情' UNION ALL
    SELECT 'SYS_ROUTER_MGMT', 'POST:/api/auth/router/add', '新增路由' UNION ALL
    SELECT 'SYS_ROUTER_MGMT', 'DELETE:/api/auth/router/delById', '删除路由' UNION ALL
    SELECT 'SYS_ROUTER_MGMT', 'POST:/api/auth/router/update', '修改路由' UNION ALL
    SELECT 'SYS_ORG_MGMT', 'POST:/api/auth/organization/pageList', '分页查询机构' UNION ALL
    SELECT 'SYS_ORG_MGMT', 'GET:/api/auth/organization/selectById', '查询机构详情' UNION ALL
    SELECT 'SYS_ORG_MGMT', 'POST:/api/auth/organization/add', '新增机构' UNION ALL
    SELECT 'SYS_ORG_MGMT', 'DELETE:/api/auth/organization/delById', '删除机构' UNION ALL
    SELECT 'SYS_ORG_MGMT', 'POST:/api/auth/organization/update', '修改机构' UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'POST:/api/auth/resource/pageList', '分页查询资源' UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'GET:/api/auth/resource/selectById', '查询资源详情' UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'POST:/api/auth/resource/add', '新增资源' UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'DELETE:/api/auth/resource/delById', '删除资源' UNION ALL
    SELECT 'SYS_RESOURCE_MGMT', 'POST:/api/auth/resource/update', '修改资源' UNION ALL
    SELECT 'SYS_OPER_LOG', 'POST:/api/auth/operLog/pageList', '分页查询操作日志' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/file/upload', '上传认证域文件' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/file/url', '获取认证域文件地址' UNION ALL
    SELECT 'SYS_PROFILE', 'POST:/api/auth/file/sha256', '计算认证域文件摘要' UNION ALL
    SELECT 'SYS_PROFILE', 'DELETE:/api/auth/file/delete', '删除认证域文件' UNION ALL
    SELECT 'SYS_ROUTER_MGMT', 'GET:/api/auth/external/router/*', '外部查询路由信息' UNION ALL
    SELECT 'SYS_ROLE_MGMT', 'GET:/api/auth/external/role/*', '外部查询角色信息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/webSocket/message', '建立消息 WebSocket 连接' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/message/query', '查询消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/message/message', '查询站内消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/message/announcement', '查询公告消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/message/todo', '查询待办消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/pageList', '分页查询消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/message/unread-count', '查询未读消息数' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/message/message/unread-count', '查询站内未读数' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/message/announcement/unread-count', '查询公告未读数' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'GET:/api/message/todo/unread-count', '查询待办未读数' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/*/read', '标记消息已读' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/read-all', '全部消息已读' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/message/read-all', '全部站内消息已读' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/announcement/read-all', '全部公告已读' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/todo/read-all', '全部待办已读' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/external/publish', '发布消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/external/publish/message', '发布站内消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/external/publish/announcement', '发布公告消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/external/publish/todo', '发布待办消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/external/revoke', '撤回消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/external/revoke/message', '撤回站内消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/external/revoke/announcement', '撤回公告消息' UNION ALL
    SELECT 'SYS_MESSAGE_CENTER', 'POST:/api/message/external/revoke/todo', '撤回待办消息' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'ANY:/api/setting/multiMessage', '外部多语言服务接口' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'POST:/api/setting/dict/type/pageList', '分页查询字典类型' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'GET:/api/setting/dict/type/selectById/*', '查询字典类型详情' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'POST:/api/setting/dict/type/save', '新增字典类型' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'POST:/api/setting/dict/type/update', '修改字典类型' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'DELETE:/api/setting/dict/type/delete', '删除字典类型' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'POST:/api/setting/dict/item/pageList', '分页查询字典项' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'GET:/api/setting/dict/item/selectById/*', '查询字典项详情' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'GET:/api/setting/dict/item/list/*', '查询字典项列表' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'POST:/api/setting/dict/item/save', '新增字典项' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'POST:/api/setting/dict/item/update', '修改字典项' UNION ALL
    SELECT 'SYS_DICT_MGMT', 'DELETE:/api/setting/dict/item/delete', '删除字典项' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'POST:/api/setting/ipWhitelist/pageList', '分页查询 IP 白名单' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'GET:/api/setting/ipWhitelist/selectById/*', '查询 IP 白名单详情' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'POST:/api/setting/ipWhitelist/save', '新增 IP 白名单' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'POST:/api/setting/ipWhitelist/update', '修改 IP 白名单' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'DELETE:/api/setting/ipWhitelist/delete', '删除 IP 白名单' UNION ALL
    SELECT 'SYS_IP_WHITELIST_MGMT', 'POST:/api/setting/ipWhitelist/warmUp', '预热 IP 白名单缓存' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'POST:/api/setting/multiMessage/language/pageList', '分页查询语言' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'GET:/api/setting/multiMessage/language/selectById/*', '查询语言详情' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'POST:/api/setting/multiMessage/language/save', '新增语言' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'POST:/api/setting/multiMessage/language/update', '修改语言' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'DELETE:/api/setting/multiMessage/language/delete', '删除语言' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'POST:/api/setting/multiMessage/message/pageList', '分页查询多语言文案' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'GET:/api/setting/multiMessage/message/selectById/*', '查询多语言文案详情' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'GET:/api/setting/multiMessage/message/list/*', '查询多语言文案列表' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'POST:/api/setting/multiMessage/message/save', '新增多语言文案' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'POST:/api/setting/multiMessage/message/update', '修改多语言文案' UNION ALL
    SELECT 'SYS_MULTI_MESSAGE_MGMT', 'DELETE:/api/setting/multiMessage/message/delete', '删除多语言文案' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'POST:/api/setting/valueSet/pageList', '分页查询值集' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'GET:/api/setting/valueSet/selectById/*', '查询值集详情' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'POST:/api/setting/valueSet/save', '新增值集' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'POST:/api/setting/valueSet/update', '修改值集' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'DELETE:/api/setting/valueSet/delete', '删除值集' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'POST:/api/setting/valueSet/item/pageList', '分页查询值集项' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'GET:/api/setting/valueSet/item/selectById/*', '查询值集项详情' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'GET:/api/setting/valueSet/item/list/*', '查询值集项列表' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'POST:/api/setting/valueSet/item/save', '新增值集项' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'POST:/api/setting/valueSet/item/update', '修改值集项' UNION ALL
    SELECT 'SYS_VALUE_SET_MGMT', 'DELETE:/api/setting/valueSet/item/delete', '删除值集项' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'POST:/api/setting/notice/pageList', '分页查询公告' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'GET:/api/setting/notice/selectById/*', '查询公告详情' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'POST:/api/setting/notice/save', '新增公告' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'POST:/api/setting/notice/update', '修改公告' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'DELETE:/api/setting/notice/delete', '删除公告' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'POST:/api/setting/notice/publish', '发布公告' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'POST:/api/setting/notice/revoke/*', '撤回公告' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'POST:/api/setting/notice/read/*/*', '标记公告已读' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'POST:/api/setting/notice/message/pageList', '分页查询公告消息' UNION ALL
    SELECT 'SYS_NOTICE_MGMT', 'POST:/api/setting/notice/message/read/*', '标记公告消息已读' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/pageList', '分页查询文件记录' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/upload/check', '校验文件上传' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/upload', '上传文件' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'GET:/api/file/internal/*', '查询文件详情' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'GET:/api/file/internal/*/url', '获取文件地址' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'DELETE:/api/file/internal/*', '删除文件' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/*/restore', '恢复文件' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'POST:/api/file/internal/storage/instance', '新增存储实例' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'PUT:/api/file/internal/storage/instance', '修改存储实例' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'DELETE:/api/file/internal/storage/instance/*', '删除存储实例' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'POST:/api/file/internal/storage/instance/*/enable', '启用存储实例' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'POST:/api/file/internal/storage/instance/*/disable', '停用存储实例' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'POST:/api/file/internal/storage/instance/testConnection', '测试存储连接' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'GET:/api/file/internal/storage/instance/*', '查询存储实例详情' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'POST:/api/file/internal/storage/instance/list', '查询存储实例列表' UNION ALL
    SELECT 'SYS_STORAGE_INSTANCE', 'GET:/api/file/internal/storage/instance/listEnabled', '查询可用存储实例' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'GET:/api/file/internal/storage/browser/*/bucket-exists', '检查存储桶' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'GET:/api/file/internal/storage/browser/*/object-exists', '检查存储对象' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'POST:/api/file/internal/storage/browser/*/list', '浏览存储对象' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'GET:/api/file/internal/storage/browser/*/stat', '查询存储对象元数据' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'POST:/api/file/internal/storage/browser/*/upload', '上传存储对象' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'POST:/api/file/internal/storage/browser/*/mkdir', '创建存储目录' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'POST:/api/file/internal/storage/browser/*/delete-object', '删除存储对象' UNION ALL
    SELECT 'SYS_OBJECT_BROWSER', 'POST:/api/file/internal/storage/browser/*/delete-directory', '删除存储目录' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/tools/sha256', '计算文件摘要' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/multipart/init', '初始化分片上传' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/multipart/part-url', '获取分片上传地址' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/multipart/complete', '完成分片上传' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/internal/multipart/abort/*', '终止分片上传' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/external/upload', '外部上传文件' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'POST:/api/file/external/sha256', '外部计算文件摘要' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'GET:/api/file/external/*', '外部查询文件详情' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'GET:/api/file/external/*/url', '外部获取文件地址' UNION ALL
    SELECT 'SYS_FILE_RECORD', 'DELETE:/api/file/external/*', '外部删除文件' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'GET:/api/monitor/*', '查询监控详情' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'GET:/api/monitor/snapshot', '查询运行监控快照' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'GET:/api/monitor/host', '查询主机监控' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'GET:/api/monitor/jvm', '查询 JVM 监控' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'GET:/api/monitor/database', '查询数据库监控' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'GET:/api/monitor/middleware', '查询中间件监控' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'GET:/api/monitor/routerInfo/*', '查询路由监控信息' UNION ALL
    SELECT 'SYS_RUNTIME_MONITOR', 'GET:/api/monitor/roleInfo/*', '查询角色监控信息'
) API_RESOURCE_SOURCE;

INSERT IGNORE INTO PEACH_AUTH_FUNCTION
    (ID, TENANT_ID, ORG_ID, PARTY_CODE, PARTY_TYPE, FUNC_CODE, FISCAL, STATE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, APP_ID, IS_DELETE)
SELECT REPLACE(UUID(), '-', ''), @TENANT_ID, @DEFAULT_ORG_ID, 'ROLE_SYS_ADMIN', 'ROLE', FUNC_CODE, 2026, 'ENABLED', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, @APP_ID, 0
FROM PEACH_FUNCTION
WHERE TENANT_ID = @TENANT_ID AND APP_ID = @APP_ID;

INSERT IGNORE INTO PEACH_AUTH_RESOURCE
    (RESOURCE_ID, TENANT_ID, ORG_ID, PARTY_CODE, FUNC_CODE, OP_TYPE, RESOURCE_CODE, RESOURCE_NAME, APP_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE, FISCAL)
SELECT REPLACE(UUID(), '-', ''), @TENANT_ID, @DEFAULT_ORG_ID, 'ROLE_SYS_ADMIN', FUNC_CODE, RESOURCE_TYPE, RESOURCE_CODE, RESOURCE_NAME, @APP_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID, 0, 2026
FROM PEACH_RESOURCE
WHERE TENANT_ID = @TENANT_ID AND APP_ID = @APP_ID;

INSERT IGNORE INTO PEACH_CODE_RULE
    (TENANT_ID, ORG_ID, CODE_PREFIX, MAX_CODE_WIDTH, CURRENT_VALUE, STATUS, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    (@TENANT_ID, @DEFAULT_ORG_ID, 'SYS', 8, 0, 'ENABLE', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    (@TENANT_ID, @BRANCH_ORG_ID, 'SYS', 8, 0, 'ENABLE', @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_DICT_TYPE
    (ID, DICT_CODE, DICT_NAME, MODULE_CODE, SORT_ORDER, STATUS, TENANT_ID, ORG_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    ('50101010101040108080808080808080', 'BOOLEAN_FLAG', '布尔标志', 'COMMON', 1, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('50202020202040209090909090909090', 'NOTICE_TYPE', '公告类型', 'SETTING', 2, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5030303030304030a0a0a0a0a0a0a0a0', 'PUBLISH_STATUS', '发布状态', 'SETTING', 3, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5040404040404040b0b0b0b0b0b0b0b0', 'MESSAGE_TYPE', '消息类型', 'SETTING', 4, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5050505050504050c0c0c0c0c0c0c0c0', 'FILE_STATUS', '文件状态', 'FILESERVICE', 5, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_DICT_ITEM
    (ID, DICT_CODE, ITEM_CODE, ITEM_VALUE, SORT_ORDER, MESSAGE_KEY, EXTRA_JSON, STATUS, TENANT_ID, ORG_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    ('51101010101040108080808080808080', 'BOOLEAN_FLAG', '0', '否', 1, 'dict.boolean.no', NULL, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('51202020202040209090909090909090', 'BOOLEAN_FLAG', '1', '是', 2, 'dict.boolean.yes', NULL, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5130303030304030a0a0a0a0a0a0a0a0', 'NOTICE_TYPE', 'INFO', '普通公告', 1, 'notice.type.info', NULL, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5140404040404040b0b0b0b0b0b0b0b0', 'NOTICE_TYPE', 'WARNING', '警告公告', 2, 'notice.type.warning', NULL, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5150505050504050c0c0c0c0c0c0c0c0', 'PUBLISH_STATUS', 'DRAFT', '草稿', 1, 'notice.publish.draft', NULL, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5160606060604060d0d0d0d0d0d0d0d0', 'PUBLISH_STATUS', 'PUBLISHED', '已发布', 2, 'notice.publish.published', NULL, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5170707070704070e0e0e0e0e0e0e0e0', 'MESSAGE_TYPE', 'NOTICE', '通知', 1, 'message.type.notice', NULL, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5180808080804080f0f0f0f0f0f0f0f0', 'FILE_STATUS', 'ACTIVE', '已生效', 1, 'file.status.active', NULL, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_LANGUAGE
    (ID, LANGUAGE_CODE, LANGUAGE_NAME, NATIVE_NAME, ICON, STATUS, DEFAULT_FLAG, SORT_ORDER, TENANT_ID, ORG_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    ('52101010101040108080808080808080', 'zh_CN', '中文（简体）', '简体中文', 'cn', 1, 1, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('52202020202040209090909090909090', 'en_US', 'English (US)', 'English', 'us', 1, 0, 2, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_VALUE_SET
    (ID, VALUE_SET_CODE, VALUE_SET_NAME, MODULE_CODE, SOURCE_TYPE, DESCRIPTION, STATUS, TENANT_ID, ORG_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    ('53101010101040108080808080808080', 'FILE_STORAGE_PROVIDER', '文件存储提供方', 'FILESERVICE', 'CUSTOM', '文件服务存储提供方', 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('53202020202040209090909090909090', 'NOTICE_PRIORITY', '公告优先级', 'SETTING', 'CUSTOM', '公告优先级配置', 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_VALUE_SET_ITEM
    (ID, VALUE_SET_CODE, ITEM_CODE, ITEM_VALUE, MESSAGE_KEY, SOURCE_TYPE, SORT_ORDER, STATUS, VISIBLE_FLAG, EXTRA_JSON, TENANT_ID, ORG_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    ('54101010101040108080808080808080', 'FILE_STORAGE_PROVIDER', 'LOCAL', '本地存储', 'value.set.file.storage.local', 'CUSTOM', 1, 1, 1, NULL, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('54202020202040209090909090909090', 'FILE_STORAGE_PROVIDER', 'MINIO', 'MinIO', 'value.set.file.storage.minio', 'CUSTOM', 2, 1, 1, NULL, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5430303030304030a0a0a0a0a0a0a0a0', 'NOTICE_PRIORITY', '1', '低', 'value.set.notice.priority.low', 'CUSTOM', 1, 1, 1, NULL, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5440404040404040b0b0b0b0b0b0b0b0', 'NOTICE_PRIORITY', '2', '中', 'value.set.notice.priority.medium', 'CUSTOM', 2, 1, 1, NULL, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('5450505050504050c0c0c0c0c0c0c0c0', 'NOTICE_PRIORITY', '3', '高', 'value.set.notice.priority.high', 'CUSTOM', 3, 1, 1, NULL, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_MULTI_MESSAGE
    (ID, MESSAGE_KEY, LOCALE, MODULE_CODE, MESSAGE_TYPE, USAGE_SCOPE, MESSAGE_CONTENT, DESCRIPTION, STATUS, TENANT_ID, ORG_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    ('55101010101040108080808080808080', 'notice.welcome.title', 'zh_CN', 'SETTING', 'TITLE', 'COMMON', '系统欢迎公告', '欢迎公告标题', 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID),
    ('55202020202040209090909090909090', 'notice.welcome.content', 'zh_CN', 'SETTING', 'TEXT', 'COMMON', '系统初始化已完成，请开始使用。', '欢迎公告内容', 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);

INSERT IGNORE INTO PEACH_NOTICE
    (ID, NOTICE_CODE, TITLE_MESSAGE_KEY, CONTENT_MESSAGE_KEY, NOTICE_TYPE, PRIORITY, PUBLISH_STATUS, EFFECTIVE_FROM, EFFECTIVE_TO, READ_COUNT, INBOX_ENABLED, STATUS, TENANT_ID, ORG_ID, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID)
VALUES
    ('56101010101040108080808080808080', 'NOTICE_INIT_001', 'notice.welcome.title', 'notice.welcome.content', 'INFO', 2, 'PUBLISHED', @NOW, NULL, 0, 1, 1, @TENANT_ID, @DEFAULT_ORG_ID, @NOW, @ADMIN_USER_ID, @NOW, @ADMIN_USER_ID);
