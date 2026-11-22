-- 修复租户菜单可见但动态路由未加载的问题。不扩大现有功能或资源授权。
START TRANSACTION;
UPDATE PEACH_ROUTER r
JOIN PEACH_FUNCTION f
  ON f.TENANT_ID = r.TENANT_ID AND f.APP_ID = r.APP_ID
 AND f.FUNC_CODE = 'SYS_TENANT_MGMT' AND f.IS_DELETE = 0
SET r.FUNC_CODE = f.FUNC_CODE
WHERE r.ROUTER_CODE = 'SYS_TENANT_MGMT'
  AND r.ROUTER_URL = '/tenant' AND r.FILE_PATH = 'system/tenant'
  AND (r.FUNC_CODE IS NULL OR r.FUNC_CODE = '');
COMMIT;
-- 已登录浏览器需要退出后重新登录，重新获取包含租户页面的路由列表。
