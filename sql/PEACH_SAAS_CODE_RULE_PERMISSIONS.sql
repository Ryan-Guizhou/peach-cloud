-- #221: Production-only SYSTEM permission definitions after DBA review and backup.
-- This migration never bootstraps a PLATFORM administrator, grants a role,
-- changes issued code counters or opens anonymous routes.
-- Verify each PERMISSION_ID and PERMISSION_CODE is unused before executing.
INSERT INTO PEACH_PERMISSION_DEFINITION
    (ID, PERMISSION_ID, TENANT_REF_ID, APP_REF_ID,
     PERMISSION_CODE, PERMISSION_NAME, RESOURCE_TYPE, OWNER_TYPE,
     DELEGABLE, STATUS, IS_DELETE, VERSION, CREATED_TIME, CREATOR_ID)
VALUES
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_CODE_PLATFORM_MANAGE', NULL, NULL,
     'PLATFORM.CODE.RULE.MANAGE', '平台全局编码规则管理', 'API', 'SYSTEM',
     0, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_CODE_TENANT_MANAGE', NULL, NULL,
     'TENANT.CODE.RULE.MANAGE', '租户独立编码规则管理', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_CODE_LIST', NULL, NULL,
     'GET:/api/auth/saas/code-rules', '查询编码规则API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_CODE_GET', NULL, NULL,
     'GET:/api/auth/saas/code-rules/{prefix}', '查看编码规则API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_CODE_PREVIEW', NULL, NULL,
     'GET:/api/auth/saas/code-rules/{prefix}/preview', '预览候选编码API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_CODE_STATUS', NULL, NULL,
     'PUT:/api/auth/saas/code-rules/{prefix}/status', '启停编码规则API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_CODE_FORMAT', NULL, NULL,
     'PUT:/api/auth/saas/code-rules/{prefix}/format', '修改编码格式API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_CODE_WATERMARK', NULL, NULL,
     'POST:/api/auth/saas/code-rules/{prefix}/watermark', '前移编码水位API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM');

-- Provision PLATFORM SYSTEM grants and matching Gateway API grants in a separate,
-- audited administrative transaction. Invalidate affected member AUTH_VERSION.
-- Tenant grants must use the existing role-management authorization flow.
