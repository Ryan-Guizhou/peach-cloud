-- #220: Production-only SYSTEM API permission definition.
-- Apply after PEACH_USER.PASSWORD_VERSION DDL and before enabling the new API.
-- An operator must separately grant this permission to each allowed active role,
-- invalidating affected AUTH_VERSION. This migration grants no privileges by itself.
-- Verify PERMISSION_ID and PERMISSION_CODE are unused before executing.
INSERT INTO PEACH_PERMISSION_DEFINITION
    (ID, PERMISSION_ID, TENANT_REF_ID, APP_REF_ID,
     PERMISSION_CODE, PERMISSION_NAME, RESOURCE_TYPE, OWNER_TYPE,
     DELEGABLE, STATUS, IS_DELETE, VERSION, CREATED_TIME, CREATOR_ID)
VALUES
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_PASSWORD_CHANGE', NULL, NULL,
     'POST:/api/auth/changePassword', '已认证用户修改密码API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM');

-- Do not grant to anonymous callers. Same-Token, current subject token and
-- database version validation are additionally enforced inside peach-auth.
