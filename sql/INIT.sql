DELETE FROM USER_OPER_LOG;
DELETE FROM PEACH_VALUE_SET_ITEM;
DELETE FROM PEACH_VALUE_SET;
DELETE FROM PEACH_SITE_MESSAGE;
DELETE FROM PEACH_NOTICE_READ_RECORD;
DELETE FROM PEACH_NOTICE;
DELETE FROM PEACH_MULTI_MESSAGE;
DELETE FROM PEACH_LANGUAGE;
DELETE FROM PEACH_DICT_ITEM;
DELETE FROM PEACH_DICT_TYPE;
DELETE FROM PEACH_FILE_UPLOAD_SESSION;
DELETE FROM PEACH_FILE_RECORD;
DELETE FROM PEACH_FILE_OBJECT;
DELETE FROM PEACH_AUTH_LOG;
DELETE FROM PEACH_AUTH_RESOURCE;
DELETE FROM PEACH_AUTH_FUNCTION;
DELETE FROM PEACH_RESOURCE;
DELETE FROM PEACH_ROUTER;
DELETE FROM PEACH_MENU;
DELETE FROM PEACH_FUNCTION;
DELETE FROM PEACH_AUTH_PARTY;
DELETE FROM PEACH_ROLE;
DELETE FROM PEACH_USER;
DELETE FROM PEACH_APPLICATION;

INSERT INTO PEACH_APPLICATION (
    APP_ID, APP_NAME, APP_TYPE, IS_OPEN, APP_DESC, LOGOUT_URL, SORT_NUM, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID, IS_DELETE
) VALUES
      ('APP_AUTH_CENTER', '系统设置', 'SYSTEM', 1, '系统设置与权限中心', NULL, '10', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin', 0),
      ('APP_CODE_GEN', '代码生成', 'SYSTEM', 1, '低代码与模板生成中心', NULL, '20', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin', 0),
      ('APP_AI_ASSIST', 'AI辅助生成代码', 'SYSTEM', 1, 'AI辅助开发工作台', NULL, '30', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin', 0),
      ('APP_MONITOR', '系统监控', 'SYSTEM', 1, '系统监控与告警中心', NULL, '40', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin', 0);

INSERT INTO PEACH_USER (
    USER_ID, USER_CODE, PASSWORD, USER_NAME, AUTH_MODE, STATUS, MENU_STYLE, MENU_ROLE,
    START_DATE, END_DATE, MOBILE_PHONE, EMAIL, IS_DELETE, IS_MODIFY, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
      ('U001', 'admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 'PASSWORD', '1', 'LEFT', 'ROLE_SYS_ADMIN', '2026-01-01', '2099-12-31', '13800000001', 'admin@peach.com', 0, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('U002', 'auth_admin', '8f20313489d6e07eb2f7b156289b3267', '权限管理员', 'PASSWORD', '1', 'LEFT', 'ROLE_AUTH_ADMIN', '2026-01-01', '2099-12-31', '13800000002', 'auth_admin@peach.com', 0, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('U003', 'codegen', '9790692abbae966b9d88c808d71ed2fa', '代码生成员', 'PASSWORD', '1', 'LEFT', 'ROLE_CODE_GEN', '2026-01-01', '2099-12-31', '13800000003', 'codegen@peach.com', 0, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('U004', 'ai_operator', '35aaafa878438f9f85e6db7ed42e0511', 'AI开发助手', 'PASSWORD', '1', 'LEFT', 'ROLE_AI_OPERATOR', '2026-01-01', '2099-12-31', '13800000004', 'ai_operator@peach.com', 0, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('U005', 'monitor', 'a808370b7b147e3533d54538bbcf13a9', '监控运维', 'PASSWORD', '1', 'LEFT', 'ROLE_MONITOR', '2026-01-01', '2099-12-31', '13800000005', 'monitor@peach.com', 0, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_ROLE (
    ROLE_ID, ROLE_CODE, FISCAL, ROLE_NAME, ROLE_DESC, ROLE_SCOPE, ROLE_TYPE, IS_DELETE, SKIP_URL, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
      ('R001', 'ROLE_SYS_ADMIN', 2026, '系统管理员', '拥有全部模块与资源权限', 'GLOBAL', 'SYSTEM', 0, '/system-setting/user', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('R002', 'ROLE_AUTH_ADMIN', 2026, '权限管理员', '负责系统设置与权限配置', 'GLOBAL', 'SYSTEM', 0, '/system-setting/user', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('R003', 'ROLE_CODE_GEN', 2026, '代码生成员', '负责模板与代码生成', 'GLOBAL', 'SYSTEM', 0, '/code-gen/workbench', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('R004', 'ROLE_AI_OPERATOR', 2026, 'AI开发助手', '负责AI生成与评审', 'GLOBAL', 'SYSTEM', 0, '/ai-assist/workbench', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('R005', 'ROLE_MONITOR', 2026, '监控运维', '负责系统监控与告警处理', 'GLOBAL', 'SYSTEM', 0, '/system-monitor/server', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_AUTH_PARTY (
    ID, ROLE_CODE, ROLE_TYPE, FISCAL, PARTY_CODE, PARTY_TYPE, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
      ('AP001', 'ROLE_SYS_ADMIN', 'SYSTEM', 2026, 'admin', 'USER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('AP002', 'ROLE_AUTH_ADMIN', 'SYSTEM', 2026, 'auth_admin', 'USER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('AP003', 'ROLE_CODE_GEN', 'SYSTEM', 2026, 'codegen', 'USER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('AP004', 'ROLE_AI_OPERATOR', 'SYSTEM', 2026, 'ai_operator', 'USER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('AP005', 'ROLE_MONITOR', 'SYSTEM', 2026, 'monitor', 'USER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_FUNCTION (
    FUNC_CODE, PARENT_FUNC_CODE, FUNC_NAME, FUNC_DESC, FUNC_URL, FUNC_SEQ, FUNC_TYPE,
    IS_MENU, IS_AUTHORIZE, APP_ID, IS_DISABLE, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
      ('SYS_SETTING', NULL, '系统设置', '系统设置根节点', '/system-setting', '001', 'CATALOG', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('SYS_USER_MGMT', 'SYS_SETTING', '用户管理', '系统用户维护', '/system-setting/user', '001001', 'MENU', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('SYS_ROLE_MGMT', 'SYS_SETTING', '角色管理', '系统角色维护', '/system-setting/role', '001002', 'MENU', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('SYS_MENU_MGMT', 'SYS_SETTING', '菜单管理', '系统菜单维护', '/system-setting/menu', '001003', 'MENU', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('SYS_FUNC_MGMT', 'SYS_SETTING', '功能管理', '系统功能维护', '/system-setting/function', '001004', 'MENU', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('SYS_ROUTER_MGMT', 'SYS_SETTING', '路由管理', '前端路由维护', '/system-setting/router', '001005', 'MENU', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('SYS_RESOURCE_MGMT', 'SYS_SETTING', '资源管理', '按钮与API资源维护', '/system-setting/resource', '001006', 'MENU', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('SYS_APP_MGMT', 'SYS_SETTING', '应用管理', '应用定义维护', '/system-setting/application', '001007', 'MENU', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('SYS_STORAGE_MGMT', 'SYS_SETTING', '存储管理', '云存储实例与对象管理', '/system-setting/storage', '001008', 'MENU', 1, 1, 'APP_AUTH_CENTER', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('CODE_GEN', NULL, '代码生成', '代码生成根节点', '/code-gen', '002', 'CATALOG', 1, 1, 'APP_CODE_GEN', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('CODE_GEN_WORKBENCH', 'CODE_GEN', '生成工作台', '代码生成工作台', '/code-gen/workbench', '002001', 'MENU', 1, 1, 'APP_CODE_GEN', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('CODE_GEN_TEMPLATE', 'CODE_GEN', '模板管理', '代码模板管理', '/code-gen/template', '002002', 'MENU', 1, 1, 'APP_CODE_GEN', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('CODE_GEN_HISTORY', 'CODE_GEN', '生成记录', '代码生成记录查询', '/code-gen/history', '002003', 'MENU', 1, 1, 'APP_CODE_GEN', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('AI_ASSIST', NULL, 'AI辅助生成代码', 'AI辅助生成代码根节点', '/ai-assist', '003', 'CATALOG', 1, 1, 'APP_AI_ASSIST', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('AI_ASSIST_WORKBENCH', 'AI_ASSIST', 'AI工作台', 'AI编码工作台', '/ai-assist/workbench', '003001', 'MENU', 1, 1, 'APP_AI_ASSIST', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('AI_PROMPT_CENTER', 'AI_ASSIST', '提示词中心', '提示词模板管理', '/ai-assist/prompt', '003002', 'MENU', 1, 1, 'APP_AI_ASSIST', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('AI_CODE_REVIEW', 'AI_ASSIST', 'AI代码评审', 'AI代码评审与建议', '/ai-assist/review', '003003', 'MENU', 1, 1, 'APP_AI_ASSIST', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('SYS_MONITOR', NULL, '系统监控', '系统监控根节点', '/system-monitor', '004', 'CATALOG', 1, 1, 'APP_MONITOR', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('MONITOR_SERVER', 'SYS_MONITOR', '服务监控', '服务运行状态监控', '/system-monitor/server', '004001', 'MENU', 1, 1, 'APP_MONITOR', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('MONITOR_LOG', 'SYS_MONITOR', '日志监控', '系统日志查询', '/system-monitor/log', '004002', 'MENU', 1, 1, 'APP_MONITOR', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('MONITOR_ALERT', 'SYS_MONITOR', '告警中心', '系统告警中心', '/system-monitor/alert', '004003', 'MENU', 1, 1, 'APP_MONITOR', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('MONITOR_STORAGE', 'SYS_MONITOR', '存储监控', '云存储实例监控', '/system-monitor/storage', '004004', 'MENU', 1, 1, 'APP_MONITOR', 0, 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_MENU (
    MENU_ID, MENU_NAME, MENU_CODE, IS_LEAF, MENU_URL, PARENT_MENU_ID, MENU_LEVEL, SORT_NO, MENU_SEQ,
    OPEN_MODE, SUBCOUNT, FUNC_CODE, MENU_APP_ID, APP_ID, IS_DELETE, IS_DISABLE, IS_SHOW, SF_BLANK, MENU_ICON,
    CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
      ('M001', '系统设置', 'MENU_SYS_SETTING', 0, '/system-setting', NULL, '1', 1, '001', 'SELF', '8', 'SYS_SETTING', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'Setting', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M002', '用户管理', 'MENU_SYS_USER', 1, '/system-setting/user', 'M001', '2', 1, '001001', 'SELF', '0', 'SYS_USER_MGMT', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'User', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M003', '角色管理', 'MENU_SYS_ROLE', 1, '/system-setting/role', 'M001', '2', 2, '001002', 'SELF', '0', 'SYS_ROLE_MGMT', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'UserSwitch', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M004', '菜单管理', 'MENU_SYS_MENU', 1, '/system-setting/menu', 'M001', '2', 3, '001003', 'SELF', '0', 'SYS_MENU_MGMT', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'Menu', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M005', '功能管理', 'MENU_SYS_FUNC', 1, '/system-setting/function', 'M001', '2', 4, '001004', 'SELF', '0', 'SYS_FUNC_MGMT', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'Grid', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M006', '路由管理', 'MENU_SYS_ROUTER', 1, '/system-setting/router', 'M001', '2', 5, '001005', 'SELF', '0', 'SYS_ROUTER_MGMT', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'Link', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M007', '资源管理', 'MENU_SYS_RESOURCE', 1, '/system-setting/resource', 'M001', '2', 6, '001006', 'SELF', '0', 'SYS_RESOURCE_MGMT', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'Pointer', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M008', '应用管理', 'MENU_SYS_APP', 1, '/system-setting/application', 'M001', '2', 7, '001007', 'SELF', '0', 'SYS_APP_MGMT', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'Appstore', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M009', '存储管理', 'MENU_SYS_STORAGE', 1, '/system-setting/storage', 'M001', '2', 8, '001008', 'SELF', '0', 'SYS_STORAGE_MGMT', 'APP_AUTH_CENTER', 'APP_AUTH_CENTER', 0, 0, 1, 0, 'Cloud', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('M101', '代码生成', 'MENU_CODE_GEN', 0, '/code-gen', NULL, '1', 2, '002', 'SELF', '3', 'CODE_GEN', 'APP_CODE_GEN', 'APP_CODE_GEN', 0, 0, 1, 0, 'CodeSandbox', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M102', '生成工作台', 'MENU_CODE_GEN_WORKBENCH', 1, '/code-gen/workbench', 'M101', '2', 1, '002001', 'SELF', '0', 'CODE_GEN_WORKBENCH', 'APP_CODE_GEN', 'APP_CODE_GEN', 0, 0, 1, 0, 'Rocket', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M103', '模板管理', 'MENU_CODE_GEN_TEMPLATE', 1, '/code-gen/template', 'M101', '2', 2, '002002', 'SELF', '0', 'CODE_GEN_TEMPLATE', 'APP_CODE_GEN', 'APP_CODE_GEN', 0, 0, 1, 0, 'FileText', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M104', '生成记录', 'MENU_CODE_GEN_HISTORY', 1, '/code-gen/history', 'M101', '2', 3, '002003', 'SELF', '0', 'CODE_GEN_HISTORY', 'APP_CODE_GEN', 'APP_CODE_GEN', 0, 0, 1, 0, 'History', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('M201', 'AI辅助生成代码', 'MENU_AI_ASSIST', 0, '/ai-assist', NULL, '1', 3, '003', 'SELF', '3', 'AI_ASSIST', 'APP_AI_ASSIST', 'APP_AI_ASSIST', 0, 0, 1, 0, 'Robot', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M202', 'AI工作台', 'MENU_AI_WORKBENCH', 1, '/ai-assist/workbench', 'M201', '2', 1, '003001', 'SELF', '0', 'AI_ASSIST_WORKBENCH', 'APP_AI_ASSIST', 'APP_AI_ASSIST', 0, 0, 1, 0, 'Bulb', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M203', '提示词中心', 'MENU_AI_PROMPT', 1, '/ai-assist/prompt', 'M201', '2', 2, '003002', 'SELF', '0', 'AI_PROMPT_CENTER', 'APP_AI_ASSIST', 'APP_AI_ASSIST', 0, 0, 1, 0, 'Message', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M204', 'AI代码评审', 'MENU_AI_REVIEW', 1, '/ai-assist/review', 'M201', '2', 3, '003003', 'SELF', '0', 'AI_CODE_REVIEW', 'APP_AI_ASSIST', 'APP_AI_ASSIST', 0, 0, 1, 0, 'Search', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('M301', '系统监控', 'MENU_SYS_MONITOR', 0, '/system-monitor', NULL, '1', 4, '004', 'SELF', '4', 'SYS_MONITOR', 'APP_MONITOR', 'APP_MONITOR', 0, 0, 1, 0, 'Monitor', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M302', '服务监控', 'MENU_MONITOR_SERVER', 1, '/system-monitor/server', 'M301', '2', 1, '004001', 'SELF', '0', 'MONITOR_SERVER', 'APP_MONITOR', 'APP_MONITOR', 0, 0, 1, 0, 'Desktop', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M303', '日志监控', 'MENU_MONITOR_LOG', 1, '/system-monitor/log', 'M301', '2', 2, '004002', 'SELF', '0', 'MONITOR_LOG', 'APP_MONITOR', 'APP_MONITOR', 0, 0, 1, 0, 'FileSearch', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M304', '告警中心', 'MENU_MONITOR_ALERT', 1, '/system-monitor/alert', 'M301', '2', 3, '004003', 'SELF', '0', 'MONITOR_ALERT', 'APP_MONITOR', 'APP_MONITOR', 0, 0, 1, 0, 'Bell', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('M305', '存储监控', 'MENU_MONITOR_STORAGE', 1, '/system-monitor/storage', 'M301', '2', 4, '004004', 'SELF', '0', 'MONITOR_STORAGE', 'APP_MONITOR', 'APP_MONITOR', 0, 0, 1, 0, 'HardDrive', '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_ROUTER (
    ROUTER_ID, ROUTER_CODE, ROUTER_NAME, ROUTER_URL, FILE_PATH, IS_AUTH, IS_CACHE, MODULE_CODE, ROUTER_LEVEL,
    CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
      ('RT002', 'SYS_USER_MGMT', '用户管理', '/system-setting/user', 'src/modules/system-setting/user/index', 1, 0, 'system-setting', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT003', 'SYS_ROLE_MGMT', '角色管理', '/system-setting/role', 'src/modules/system-setting/role/index', 1, 0, 'system-setting', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT004', 'SYS_MENU_MGMT', '菜单管理', '/system-setting/menu', 'src/modules/system-setting/menu/index', 1, 0, 'system-setting', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT005', 'SYS_FUNC_MGMT', '功能管理', '/system-setting/function', 'src/modules/system-setting/function/index', 1, 0, 'system-setting', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT006', 'SYS_ROUTER_MGMT', '路由管理', '/system-setting/router', 'src/modules/system-setting/router/index', 1, 0, 'system-setting', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT007', 'SYS_RESOURCE_MGMT', '资源管理', '/system-setting/resource', 'src/modules/system-setting/resource/index', 1, 0, 'system-setting', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT008', 'SYS_APP_MGMT', '应用管理', '/system-setting/application', 'src/modules/system-setting/application/index', 1, 0, 'system-setting', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT009', 'SYS_STORAGE_MGMT', '存储管理', '/system-setting/storage', 'src/modules/system-setting/storage/index', 1, 0, 'system-setting', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT102', 'CODE_GEN_WORKBENCH', '生成工作台', '/code-gen/workbench', 'src/modules/code-gen/workbench/index', 1, 1, 'code-gen', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT103', 'CODE_GEN_TEMPLATE', '模板管理', '/code-gen/template', 'src/modules/code-gen/template/index', 1, 0, 'code-gen', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT104', 'CODE_GEN_HISTORY', '生成记录', '/code-gen/history', 'src/modules/code-gen/history/index', 1, 0, 'code-gen', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT202', 'AI_ASSIST_WORKBENCH', 'AI工作台', '/ai-assist/workbench', 'src/modules/ai-assist/workbench/index', 1, 1, 'ai-assist', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT203', 'AI_PROMPT_CENTER', '提示词中心', '/ai-assist/prompt', 'src/modules/ai-assist/prompt/index', 1, 0, 'ai-assist', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT204', 'AI_CODE_REVIEW', 'AI代码评审', '/ai-assist/review', 'src/modules/ai-assist/review/index', 1, 0, 'ai-assist', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT302', 'MONITOR_SERVER', '服务监控', '/system-monitor/server', 'src/modules/system-monitor/server/index', 1, 1, 'system-monitor', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT303', 'MONITOR_LOG', '日志监控', '/system-monitor/log', 'src/modules/system-monitor/log/index', 1, 0, 'system-monitor', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT304', 'MONITOR_ALERT', '告警中心', '/system-monitor/alert', 'src/modules/system-monitor/alert/index', 1, 0, 'system-monitor', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RT305', 'MONITOR_STORAGE', '存储监控', '/system-monitor/storage', 'src/modules/system-monitor/storage/index', 1, 0, 'system-monitor', 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_RESOURCE (
    RESOURCE_ID, FUNC_CODE, RESOURCE_TYPE, RESOURCE_CODE, RESOURCE_NAME, APP_ID, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
      ('RS001', 'SYS_USER_MGMT', 'BUTTON', 'BTN_SYS_USER_QUERY', '用户查询按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS002', 'SYS_USER_MGMT', 'BUTTON', 'BTN_SYS_USER_SAVE', '用户保存按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS003', 'SYS_USER_MGMT', 'API', '/api/auth/user/page', '用户分页查询接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS004', 'SYS_USER_MGMT', 'API', '/api/auth/user/save', '用户保存接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS005', 'SYS_ROLE_MGMT', 'BUTTON', 'BTN_SYS_ROLE_QUERY', '角色查询按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS006', 'SYS_ROLE_MGMT', 'BUTTON', 'BTN_SYS_ROLE_SAVE', '角色保存按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS007', 'SYS_ROLE_MGMT', 'API', '/api/auth/role/page', '角色分页查询接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS008', 'SYS_ROLE_MGMT', 'API', '/api/auth/role/save', '角色保存接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS009', 'SYS_MENU_MGMT', 'BUTTON', 'BTN_SYS_MENU_QUERY', '菜单查询按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS010', 'SYS_MENU_MGMT', 'BUTTON', 'BTN_SYS_MENU_SAVE', '菜单保存按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS011', 'SYS_MENU_MGMT', 'API', '/api/auth/menu/tree', '菜单树查询接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS012', 'SYS_MENU_MGMT', 'API', '/api/auth/menu/save', '菜单保存接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS013', 'SYS_FUNC_MGMT', 'BUTTON', 'BTN_SYS_FUNC_QUERY', '功能查询按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS014', 'SYS_FUNC_MGMT', 'BUTTON', 'BTN_SYS_FUNC_SAVE', '功能保存按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS015', 'SYS_FUNC_MGMT', 'API', '/api/auth/function/tree', '功能树查询接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS016', 'SYS_FUNC_MGMT', 'API', '/api/auth/function/save', '功能保存接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS017', 'SYS_ROUTER_MGMT', 'BUTTON', 'BTN_SYS_ROUTER_QUERY', '路由查询按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS018', 'SYS_ROUTER_MGMT', 'BUTTON', 'BTN_SYS_ROUTER_SAVE', '路由保存按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS019', 'SYS_ROUTER_MGMT', 'API', '/api/auth/router/page', '路由分页查询接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS020', 'SYS_ROUTER_MGMT', 'API', '/api/auth/router/save', '路由保存接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS021', 'SYS_RESOURCE_MGMT', 'BUTTON', 'BTN_SYS_RESOURCE_QUERY', '资源查询按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS022', 'SYS_RESOURCE_MGMT', 'BUTTON', 'BTN_SYS_RESOURCE_SAVE', '资源保存按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS023', 'SYS_RESOURCE_MGMT', 'API', '/api/auth/resource/page', '资源分页查询接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS024', 'SYS_RESOURCE_MGMT', 'API', '/api/auth/resource/save', '资源保存接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS025', 'SYS_APP_MGMT', 'BUTTON', 'BTN_SYS_APP_QUERY', '应用查询按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS026', 'SYS_APP_MGMT', 'BUTTON', 'BTN_SYS_APP_SAVE', '应用保存按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS027', 'SYS_APP_MGMT', 'API', '/api/auth/application/page', '应用分页查询接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS028', 'SYS_APP_MGMT', 'API', '/api/auth/application/save', '应用保存接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS029', 'SYS_STORAGE_MGMT', 'BUTTON', 'BTN_SYS_STORAGE_QUERY', '存储管理查询按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS030', 'SYS_STORAGE_MGMT', 'BUTTON', 'BTN_SYS_STORAGE_SAVE', '存储管理保存按钮', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS031', 'SYS_STORAGE_MGMT', 'API', '/api/file/cloud/storage/instance/list', '存储实例分页查询接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS032', 'SYS_STORAGE_MGMT', 'API', '/api/file/cloud/storage/instance/save', '存储实例保存接口', 'APP_AUTH_CENTER', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('RS101', 'CODE_GEN_WORKBENCH', 'BUTTON', 'BTN_CODE_GEN_RUN', '执行生成按钮', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS102', 'CODE_GEN_WORKBENCH', 'BUTTON', 'BTN_CODE_GEN_PREVIEW', '生成预览按钮', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS103', 'CODE_GEN_WORKBENCH', 'API', '/api/codegen/run', '执行代码生成接口', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS104', 'CODE_GEN_WORKBENCH', 'API', '/api/codegen/preview', '预览代码生成接口', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS105', 'CODE_GEN_TEMPLATE', 'BUTTON', 'BTN_CODE_TEMPLATE_QUERY', '模板查询按钮', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS106', 'CODE_GEN_TEMPLATE', 'BUTTON', 'BTN_CODE_TEMPLATE_SAVE', '模板保存按钮', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS107', 'CODE_GEN_TEMPLATE', 'API', '/api/codegen/template/page', '模板分页查询接口', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS108', 'CODE_GEN_TEMPLATE', 'API', '/api/codegen/template/save', '模板保存接口', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS109', 'CODE_GEN_HISTORY', 'BUTTON', 'BTN_CODE_HISTORY_QUERY', '生成记录查询按钮', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS110', 'CODE_GEN_HISTORY', 'BUTTON', 'BTN_CODE_HISTORY_EXPORT', '生成记录导出按钮', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS111', 'CODE_GEN_HISTORY', 'API', '/api/codegen/history/page', '生成记录分页查询接口', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS112', 'CODE_GEN_HISTORY', 'API', '/api/codegen/history/export', '生成记录导出接口', 'APP_CODE_GEN', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('RS201', 'AI_ASSIST_WORKBENCH', 'BUTTON', 'BTN_AI_CODE_GENERATE', 'AI生成代码按钮', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS202', 'AI_ASSIST_WORKBENCH', 'BUTTON', 'BTN_AI_CODE_ACCEPT', 'AI采纳建议按钮', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS203', 'AI_ASSIST_WORKBENCH', 'API', '/api/ai/code/generate', 'AI生成代码接口', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS204', 'AI_ASSIST_WORKBENCH', 'API', '/api/ai/code/accept', 'AI采纳建议接口', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS205', 'AI_PROMPT_CENTER', 'BUTTON', 'BTN_AI_PROMPT_QUERY', '提示词查询按钮', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS206', 'AI_PROMPT_CENTER', 'BUTTON', 'BTN_AI_PROMPT_SAVE', '提示词保存按钮', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS207', 'AI_PROMPT_CENTER', 'API', '/api/ai/prompt/page', '提示词分页查询接口', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS208', 'AI_PROMPT_CENTER', 'API', '/api/ai/prompt/save', '提示词保存接口', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS209', 'AI_CODE_REVIEW', 'BUTTON', 'BTN_AI_REVIEW_RUN', 'AI评审执行按钮', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS210', 'AI_CODE_REVIEW', 'BUTTON', 'BTN_AI_REVIEW_EXPORT', 'AI评审导出按钮', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS211', 'AI_CODE_REVIEW', 'API', '/api/ai/review/run', 'AI评审执行接口', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS212', 'AI_CODE_REVIEW', 'API', '/api/ai/review/export', 'AI评审导出接口', 'APP_AI_ASSIST', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),

      ('RS301', 'MONITOR_SERVER', 'BUTTON', 'BTN_MONITOR_SERVER_QUERY', '服务监控查询按钮', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS302', 'MONITOR_SERVER', 'BUTTON', 'BTN_MONITOR_SERVER_REFRESH', '服务监控刷新按钮', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS303', 'MONITOR_SERVER', 'API', '/api/monitor/server/page', '服务监控分页查询接口', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS304', 'MONITOR_SERVER', 'API', '/api/monitor/server/refresh', '服务监控刷新接口', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS305', 'MONITOR_LOG', 'BUTTON', 'BTN_MONITOR_LOG_QUERY', '日志查询按钮', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS306', 'MONITOR_LOG', 'BUTTON', 'BTN_MONITOR_LOG_EXPORT', '日志导出按钮', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS307', 'MONITOR_LOG', 'API', '/api/monitor/log/page', '日志分页查询接口', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS308', 'MONITOR_LOG', 'API', '/api/monitor/log/export', '日志导出接口', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS309', 'MONITOR_ALERT', 'BUTTON', 'BTN_MONITOR_ALERT_QUERY', '告警查询按钮', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS310', 'MONITOR_ALERT', 'BUTTON', 'BTN_MONITOR_ALERT_HANDLE', '告警处理按钮', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS311', 'MONITOR_ALERT', 'API', '/api/monitor/alert/page', '告警分页查询接口', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS312', 'MONITOR_ALERT', 'API', '/api/monitor/alert/handle', '告警处理接口', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS313', 'MONITOR_STORAGE', 'BUTTON', 'BTN_MONITOR_STORAGE_QUERY', '存储监控查询按钮', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS314', 'MONITOR_STORAGE', 'BUTTON', 'BTN_MONITOR_STORAGE_REFRESH', '存储监控刷新按钮', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS315', 'MONITOR_STORAGE', 'API', '/api/monitor/storage/enabled/list', '存储监控实例列表接口', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
      ('RS316', 'MONITOR_STORAGE', 'API', '/api/monitor/storage/{instanceId}', '存储监控详情接口', 'APP_MONITOR', 0, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_AUTH_FUNCTION (
    ID, PARTY_CODE, PARTY_TYPE, FUNC_CODE, FISCAL, STATE, APP_ID, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AF_', FUNC_CODE),
    'ROLE_SYS_ADMIN',
    'ROLE',
    FUNC_CODE,
    2026,
    'ENABLED',
    APP_ID,
    0,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_FUNCTION;

INSERT INTO PEACH_AUTH_FUNCTION (
    ID, PARTY_CODE, PARTY_TYPE, FUNC_CODE, FISCAL, STATE, APP_ID, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AF_AUTH_', FUNC_CODE),
    'ROLE_AUTH_ADMIN',
    'ROLE',
    FUNC_CODE,
    2026,
    'ENABLED',
    APP_ID,
    0,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_FUNCTION
WHERE FUNC_CODE IN ('SYS_SETTING', 'SYS_USER_MGMT', 'SYS_ROLE_MGMT', 'SYS_MENU_MGMT', 'SYS_FUNC_MGMT', 'SYS_ROUTER_MGMT', 'SYS_RESOURCE_MGMT', 'SYS_APP_MGMT', 'SYS_STORAGE_MGMT');

INSERT INTO PEACH_AUTH_FUNCTION (
    ID, PARTY_CODE, PARTY_TYPE, FUNC_CODE, FISCAL, STATE, APP_ID, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AF_CODE_', FUNC_CODE),
    'ROLE_CODE_GEN',
    'ROLE',
    FUNC_CODE,
    2026,
    'ENABLED',
    APP_ID,
    0,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_FUNCTION
WHERE FUNC_CODE IN ('CODE_GEN', 'CODE_GEN_WORKBENCH', 'CODE_GEN_TEMPLATE', 'CODE_GEN_HISTORY');

INSERT INTO PEACH_AUTH_FUNCTION (
    ID, PARTY_CODE, PARTY_TYPE, FUNC_CODE, FISCAL, STATE, APP_ID, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AF_AI_', FUNC_CODE),
    'ROLE_AI_OPERATOR',
    'ROLE',
    FUNC_CODE,
    2026,
    'ENABLED',
    APP_ID,
    0,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_FUNCTION
WHERE FUNC_CODE IN ('AI_ASSIST', 'AI_ASSIST_WORKBENCH', 'AI_PROMPT_CENTER', 'AI_CODE_REVIEW');

INSERT INTO PEACH_AUTH_FUNCTION (
    ID, PARTY_CODE, PARTY_TYPE, FUNC_CODE, FISCAL, STATE, APP_ID, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AF_MON_', FUNC_CODE),
    'ROLE_MONITOR',
    'ROLE',
    FUNC_CODE,
    2026,
    'ENABLED',
    APP_ID,
    0,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_FUNCTION
WHERE FUNC_CODE IN ('SYS_MONITOR', 'MONITOR_SERVER', 'MONITOR_LOG', 'MONITOR_ALERT', 'MONITOR_STORAGE');

INSERT INTO PEACH_AUTH_RESOURCE (
    RESOURCE_ID, PARTY_CODE, FUNC_CODE, OP_TYPE, RESOURCE_CODE, RESOURCE_NAME, APP_ID, IS_DELETE, FISCAL, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AR_', RESOURCE_ID),
    'ROLE_SYS_ADMIN',
    FUNC_CODE,
    RESOURCE_TYPE,
    RESOURCE_CODE,
    RESOURCE_NAME,
    APP_ID,
    0,
    2026,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_RESOURCE;

INSERT INTO PEACH_AUTH_RESOURCE (
    RESOURCE_ID, PARTY_CODE, FUNC_CODE, OP_TYPE, RESOURCE_CODE, RESOURCE_NAME, APP_ID, IS_DELETE, FISCAL, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AR_AUTH_', RESOURCE_ID),
    'ROLE_AUTH_ADMIN',
    FUNC_CODE,
    RESOURCE_TYPE,
    RESOURCE_CODE,
    RESOURCE_NAME,
    APP_ID,
    0,
    2026,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_RESOURCE
WHERE APP_ID = 'APP_AUTH_CENTER';

INSERT INTO PEACH_AUTH_RESOURCE (
    RESOURCE_ID, PARTY_CODE, FUNC_CODE, OP_TYPE, RESOURCE_CODE, RESOURCE_NAME, APP_ID, IS_DELETE, FISCAL, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AR_CODE_', RESOURCE_ID),
    'ROLE_CODE_GEN',
    FUNC_CODE,
    RESOURCE_TYPE,
    RESOURCE_CODE,
    RESOURCE_NAME,
    APP_ID,
    0,
    2026,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_RESOURCE
WHERE APP_ID = 'APP_CODE_GEN';

INSERT INTO PEACH_AUTH_RESOURCE (
    RESOURCE_ID, PARTY_CODE, FUNC_CODE, OP_TYPE, RESOURCE_CODE, RESOURCE_NAME, APP_ID, IS_DELETE, FISCAL, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AR_AI_', RESOURCE_ID),
    'ROLE_AI_OPERATOR',
    FUNC_CODE,
    RESOURCE_TYPE,
    RESOURCE_CODE,
    RESOURCE_NAME,
    APP_ID,
    0,
    2026,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_RESOURCE
WHERE APP_ID = 'APP_AI_ASSIST';

INSERT INTO PEACH_AUTH_RESOURCE (
    RESOURCE_ID, PARTY_CODE, FUNC_CODE, OP_TYPE, RESOURCE_CODE, RESOURCE_NAME, APP_ID, IS_DELETE, FISCAL, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
)
SELECT
    CONCAT('AR_MON_', RESOURCE_ID),
    'ROLE_MONITOR',
    FUNC_CODE,
    RESOURCE_TYPE,
    RESOURCE_CODE,
    RESOURCE_NAME,
    APP_ID,
    0,
    2026,
    '2026-04-05 00:00:00',
    'admin',
    '2026-04-05 00:00:00',
    'admin'
FROM PEACH_RESOURCE
WHERE APP_ID = 'APP_MONITOR';



--
INSERT INTO PEACH_DICT_TYPE (
    ID, DICT_CODE, DICT_NAME, MODULE_CODE, SORT_ORDER, STATUS, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('DT001', 'BOOLEAN_FLAG', '布尔标志', 'COMMON', 1, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT002', 'LANGUAGE_STATUS', '语言状态', 'SETTING', 2, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT003', 'NOTICE_TYPE', '公告类型', 'SETTING', 3, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT004', 'PUBLISH_STATUS', '发布状态', 'SETTING', 4, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT005', 'MESSAGE_TYPE', '消息类型', 'SETTING', 5, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT006', 'MESSAGE_SOURCE_TYPE', '消息来源类型', 'SETTING', 6, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT007', 'VALUE_SET_SOURCE_TYPE', '值集来源类型', 'SETTING', 7, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT008', 'FILE_STATUS', '文件状态', 'FILESERVICE', 8, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT009', 'STORAGE_STATUS', '存储状态', 'FILESERVICE', 9, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DT010', 'SESSION_STATUS', '上传会话状态', 'FILESERVICE', 10, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_DICT_ITEM (
    ID, DICT_CODE, ITEM_CODE, ITEM_VALUE, SORT_ORDER, MESSAGE_KEY, EXTRA_JSON, STATUS, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('DI001', 'BOOLEAN_FLAG', '0', '否', 1, 'dict.boolean.no', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI002', 'BOOLEAN_FLAG', '1', '是', 2, 'dict.boolean.yes', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI003', 'LANGUAGE_STATUS', '0', '禁用', 1, 'dict.language.disabled', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI004', 'LANGUAGE_STATUS', '1', '启用', 2, 'dict.language.enabled', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI005', 'NOTICE_TYPE', 'INFO', '普通公告', 1, 'notice.type.info', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI006', 'NOTICE_TYPE', 'WARNING', '警告公告', 2, 'notice.type.warning', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI007', 'NOTICE_TYPE', 'MAINTENANCE', '维护公告', 3, 'notice.type.maintenance', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI008', 'NOTICE_TYPE', 'PROMOTION', '活动公告', 4, 'notice.type.promotion', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI009', 'PUBLISH_STATUS', 'DRAFT', '草稿', 1, 'notice.publish.draft', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI010', 'PUBLISH_STATUS', 'PUBLISHED', '已发布', 2, 'notice.publish.published', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI011', 'PUBLISH_STATUS', 'REVOKED', '已撤销', 3, 'notice.publish.revoked', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI012', 'PUBLISH_STATUS', 'OFFLINE', '已下线', 4, 'notice.publish.offline', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI013', 'MESSAGE_TYPE', 'NOTICE', '通知', 1, 'message.type.notice', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI014', 'MESSAGE_TYPE', 'ANNOUNCEMENT', '公告', 2, 'message.type.announcement', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI015', 'MESSAGE_TYPE', 'SYSTEM', '系统消息', 3, 'message.type.system', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI016', 'MESSAGE_TYPE', 'TODO', '待办', 4, 'message.type.todo', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI017', 'MESSAGE_SOURCE_TYPE', 'CUSTOM', '自定义', 1, 'message.source.custom', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI018', 'MESSAGE_SOURCE_TYPE', 'ANNOUNCEMENT', '公告', 2, 'message.source.announcement', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI019', 'VALUE_SET_SOURCE_TYPE', 'DICT', '字典', 1, 'value.set.source.dict', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI020', 'VALUE_SET_SOURCE_TYPE', 'CUSTOM', '自定义', 2, 'value.set.source.custom', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI021', 'FILE_STATUS', 'UPLOAD_PENDING', '上传中', 1, 'file.status.upload_pending', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI022', 'FILE_STATUS', 'ACTIVE', '已生效', 2, 'file.status.active', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI023', 'FILE_STATUS', 'DELETED', '已删除', 3, 'file.status.deleted', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI024', 'FILE_STATUS', 'UPLOAD_FAILED', '上传失败', 4, 'file.status.upload_failed', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI025', 'STORAGE_STATUS', 'UPLOADING', '上传中', 1, 'storage.status.uploading', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI026', 'STORAGE_STATUS', 'ACTIVE', '已生效', 2, 'storage.status.active', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI027', 'STORAGE_STATUS', 'DELETE_PENDING', '待删除', 3, 'storage.status.delete_pending', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI028', 'STORAGE_STATUS', 'DELETED', '已删除', 4, 'storage.status.deleted', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI029', 'STORAGE_STATUS', 'UPLOAD_FAILED', '上传失败', 5, 'storage.status.upload_failed', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI030', 'SESSION_STATUS', 'INITIATED', '已初始化', 1, 'session.status.initiated', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI031', 'SESSION_STATUS', 'UPLOADING', '上传中', 2, 'session.status.uploading', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI032', 'SESSION_STATUS', 'COMPLETED', '已完成', 3, 'session.status.completed', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI033', 'SESSION_STATUS', 'ABORTED', '已中止', 4, 'session.status.aborted', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI034', 'SESSION_STATUS', 'EXPIRED', '已过期', 5, 'session.status.expired', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('DI035', 'SESSION_STATUS', 'FAILED', '已失败', 6, 'session.status.failed', NULL, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_LANGUAGE (
    ID, LANGUAGE_CODE, LANGUAGE_NAME, NATIVE_NAME, ICON, STATUS, DEFAULT_FLAG, SORT_ORDER, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('LG001', 'zh_CN', '中文(简体)', '简体中文', 'cn', 1, 1, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('LG002', 'en_US', 'English (US)', 'English', 'us', 1, 0, 2, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_VALUE_SET (
    ID, VALUE_SET_CODE, VALUE_SET_NAME, MODULE_CODE, SOURCE_TYPE, DESCRIPTION, STATUS, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('VS001', 'FILE_STORAGE_PROVIDER', '文件存储提供方', 'FILESERVICE', 'CUSTOM', '文件服务上传提供方枚举', 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('VS002', 'NOTICE_PRIORITY', '公告优先级', 'SETTING', 'CUSTOM', '公告优先级配置', 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_VALUE_SET_ITEM (
    ID, VALUE_SET_CODE, ITEM_CODE, ITEM_VALUE, MESSAGE_KEY, SOURCE_TYPE, SORT_ORDER, STATUS, VISIBLE_FLAG, EXTRA_JSON, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('VSI001', 'FILE_STORAGE_PROVIDER', 'LOCAL', '本地存储', 'value.set.file.storage.local', 'CUSTOM', 1, 1, 1, NULL, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('VSI002', 'FILE_STORAGE_PROVIDER', 'MINIO', 'MinIO', 'value.set.file.storage.minio', 'CUSTOM', 2, 1, 1, NULL, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('VSI003', 'NOTICE_PRIORITY', '1', '低', 'value.set.notice.priority.low', 'CUSTOM', 1, 1, 1, NULL, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('VSI004', 'NOTICE_PRIORITY', '2', '中', 'value.set.notice.priority.medium', 'CUSTOM', 2, 1, 1, NULL, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('VSI005', 'NOTICE_PRIORITY', '3', '高', 'value.set.notice.priority.high', 'CUSTOM', 3, 1, 1, NULL, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_MULTI_MESSAGE (
    ID, MESSAGE_KEY, LOCALE, MODULE_CODE, MESSAGE_TYPE, USAGE_SCOPE, MESSAGE_CONTENT, DESCRIPTION, STATUS, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('MM001', 'notice.welcome.title', 'zh_CN', 'SETTING', 'TITLE', 'COMMON', '系统欢迎公告', '欢迎公告标题', 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('MM002', 'notice.welcome.title', 'en_US', 'SETTING', 'TITLE', 'COMMON', 'System Welcome Notice', 'Welcome notice title', 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('MM003', 'notice.welcome.content', 'zh_CN', 'SETTING', 'TEXT', 'COMMON', '系统初始化已完成，请开始使用。', '欢迎公告内容', 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin'),
    ('MM004', 'notice.welcome.content', 'en_US', 'SETTING', 'TEXT', 'COMMON', 'System initialization has been completed.', 'Welcome notice content', 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_NOTICE (
    ID, NOTICE_CODE, TITLE_MESSAGE_KEY, CONTENT_MESSAGE_KEY, NOTICE_TYPE, PRIORITY, PUBLISH_STATUS, EFFECTIVE_FROM, EFFECTIVE_TO, READ_COUNT, INBOX_ENABLED, STATUS, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('N001', 'NOTICE_20260405_001', 'notice.welcome.title', 'notice.welcome.content', 'INFO', 2, 'PUBLISHED', '2026-04-05 00:00:00', NULL, 0, 1, 1, '2026-04-05 00:00:00', 'admin', '2026-04-05 00:00:00', 'admin');

INSERT INTO PEACH_NOTICE_READ_RECORD (
    ID, NOTICE_CODE, READ_USER_ID, READ_TIME, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('NR001', 'NOTICE_20260405_001', 'U001', '2026-04-05 08:00:00', '2026-04-05 08:00:00', 'admin', '2026-04-05 08:00:00', 'admin');

INSERT INTO PEACH_SITE_MESSAGE (
    ID, MESSAGE_CODE, RECEIVER_ID, TITLE_MESSAGE_KEY, CONTENT_MESSAGE_KEY, MESSAGE_TYPE, SOURCE_TYPE, SOURCE_CODE, READ_FLAG, SEND_STATUS, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('SM001', 'MSG_20260405_001', 'U001', 'notice.welcome.title', 'notice.welcome.content', 'NOTICE', 'ANNOUNCEMENT', 'NOTICE_20260405_001', 0, 'SENT', '2026-04-05 08:00:00', 'admin', '2026-04-05 08:00:00', 'admin');

INSERT INTO PEACH_FILE_OBJECT (
    OBJECT_ID, HASH_SHA256, HASH_MD5, FILE_SIZE, STORAGE_PROVIDER, BUCKET_NAME, OBJECT_KEY, ORIGIN_FILE_NAME, CONTENT_TYPE, EXTENSION, STORAGE_STATUS, REF_COUNT, UPLOAD_TIME, LAST_ACCESS_TIME, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('OBJ001', 'sha256-demo-001', 'md5-demo-001', 1024, 'LOCAL', 'default', 'demo/2026/04/05/readme.txt', 'readme.txt', 'text/plain', 'txt', 'ACTIVE', 1, '2026-04-05 08:00:00', '2026-04-05 08:00:00', 0, '2026-04-05 08:00:00', 'admin', '2026-04-05 08:00:00', 'admin');

INSERT INTO PEACH_FILE_RECORD (
    FILE_ID, OBJECT_ID, BIZ_TYPE, BIZ_ID, BIZ_TAG, FILE_NAME, DISPLAY_NAME, CONTENT_TYPE, FILE_SIZE, FILE_EXT, FILE_STATUS, DELETE_TIME, EXPIRE_DELETE_TIME, REMARK, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('FILE001', 'OBJ001', 'NOTICE', 'NOTICE_20260405_001', 'DEFAULT', 'readme.txt', 'readme.txt', 'text/plain', 1024, 'txt', 'ACTIVE', NULL, NULL, '初始化示例文件', 0, '2026-04-05 08:00:00', 'admin', '2026-04-05 08:00:00', 'admin');

INSERT INTO PEACH_FILE_UPLOAD_SESSION (
    SESSION_ID, FILE_ID, OBJECT_ID, HASH_SHA256, HASH_MD5, FILE_SIZE, FILE_NAME, DISPLAY_NAME, CONTENT_TYPE, BIZ_TYPE, BIZ_ID, BIZ_TAG, REMARK, STORAGE_PROVIDER, BUCKET_NAME, OBJECT_KEY, UPLOAD_ID, SESSION_STATUS, EXPIRE_TIME, IS_DELETE, CREATED_TIME, CREATOR_ID, MODIFY_TIME, MODIFIER_ID
) VALUES
    ('SES001', 'FILE001', 'OBJ001', 'sha256-demo-001', 'md5-demo-001', 1024, 'readme.txt', 'readme.txt', 'text/plain', 'NOTICE', 'NOTICE_20260405_001', 'DEFAULT', '初始化上传会话', 'LOCAL', 'default', 'demo/2026/04/05/readme.txt', 'UPLOAD-001', 'COMPLETED', '2026-04-06 08:00:00', 0, '2026-04-05 08:00:00', 'admin', '2026-04-05 08:00:00', 'admin');

INSERT INTO PEACH_AUTH_LOG (
    LOG_ID, OPERATOR_CODE, OPERATOR_NAME, USER_CODE, USER_NAME, AUTH_DESCRIBE, OPERAT_TIME
) VALUES
    ('AL001', 'admin', '系统管理员', 'auth_admin', '权限管理员', '初始化授权记录', '2026-04-05 08:00:00');

INSERT INTO USER_OPER_LOG (
    ID, OPT_TYPE_CODE, MODULE_CODE, CREATOR_CODE, CREATOR_NAME, OPT_CONTENT, CREATE_TIME, OPT_LEVEL, PRIVATE_IP, PUBLIC_IP, DEVICE, BROWSER, OS, EXECUTION_TIME, IS_SUCCESS, ERROR_MSG, RESPONSE_DATA, ROLE_CODE, REQUEST_URI, REQUEST_METHOD
) VALUES
    ('UL001', 'LOGIN', 'SYSTEM', 'admin', '系统管理员', '初始化登录日志', '2026-04-05 08:00:00', 'INFO', '192.168.1.10', '127.0.0.1', 'Windows PC', 'Chrome', 'Windows 11', 120, 'Y', NULL, NULL, 'ROLE_SYS_ADMIN', '/api/login', 'POST');
