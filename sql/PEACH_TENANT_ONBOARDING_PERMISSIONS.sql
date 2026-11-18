-- #219: Run only through a privileged production migration after backup and review.
-- This script defines SYSTEM permissions only. It never creates an administrator,
-- binds a role or enables anonymous tenant bootstrap.
-- Before running, check that each PERMISSION_ID and PERMISSION_CODE is unused.
-- After running, verify OWNER_TYPE=SYSTEM, TENANT_REF_ID/APP_REF_ID=NULL,
-- and DELEGABLE=0 for both PLATFORM entries. Provision the PLATFORM tenant,
-- its trusted operator and SYSTEM role by a separate audited DBA process.
INSERT INTO PEACH_PERMISSION_DEFINITION
    (ID, PERMISSION_ID, TENANT_REF_ID, APP_REF_ID,
     PERMISSION_CODE, PERMISSION_NAME, RESOURCE_TYPE, OWNER_TYPE,
     DELEGABLE, STATUS, IS_DELETE, VERSION, CREATED_TIME, CREATOR_ID)
VALUES
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_PLATFORM_CREATE', NULL, NULL,
     'PLATFORM.TENANT.CREATE', '平台可信租户创建权限', 'API', 'SYSTEM',
     0, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_PLATFORM_API', NULL, NULL,
     'POST:/api/auth/saas/platform/tenants', '平台创建租户API', 'API', 'SYSTEM',
     0, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_MEMBER_INVITE', NULL, NULL,
     'POST:/api/auth/saas/tenants/members/invitations', '邀请成员API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_MEMBER_CANCEL', NULL, NULL,
     'DELETE:/api/auth/saas/tenants/members/invitations/{invitationRefId}', '取消邀请API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_MEMBER_ACCEPT', NULL, NULL,
     'POST:/api/auth/saas/tenants/members/invitations/{invitationRefId}/accept', '接受邀请API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_MEMBER_DISABLE', NULL, NULL,
     'POST:/api/auth/saas/tenants/members/{memberRefId}/disable', '停用成员API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_MEMBER_ENABLE', NULL, NULL,
     'POST:/api/auth/saas/tenants/members/{memberRefId}/enable', '恢复成员API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_MEMBER_LEAVE', NULL, NULL,
     'POST:/api/auth/saas/tenants/members/leave', '当前成员退出API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_ORG_CREATE', NULL, NULL,
     'POST:/api/auth/saas/tenants/organizations', '创建机构API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_ORG_UPDATE', NULL, NULL,
     'PUT:/api/auth/saas/tenants/organizations/{orgRefId}', '更新机构API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_ORG_DELETE', NULL, NULL,
     'DELETE:/api/auth/saas/tenants/organizations/{orgRefId}', '删除机构API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM'),
    (REPLACE(UUID(), '-', ''), 'PERM_SAAS_ORG_MOVE', NULL, NULL,
     'POST:/api/auth/saas/tenants/organizations/{orgRefId}/move', '移动机构API', 'API', 'SYSTEM',
     1, 'ACTIVE', 0, 0,
     DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), 'SYSTEM');

-- Every new tenant owner receives only globally delegable SYSTEM permissions.
-- No PLATFORM.TENANT.CREATE or platform API permission is delegated to owners.
-- Existing tenant role authorizations must be migrated explicitly with
-- tenant/app scope validation and authorization-version invalidation.
