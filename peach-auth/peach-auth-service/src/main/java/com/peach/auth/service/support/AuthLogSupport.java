package com.peach.auth.service.support;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dao.RoleDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.entity.AuthLogDO;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.service.IAuthLogService;
import com.peach.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Component;

/**
 * 授权日志写入辅助，补全操作人与被授权对象必填字段。
 */
@Indexed
@Component
@RequiredArgsConstructor
public class AuthLogSupport {

    private static final String UNKNOWN = "UNKNOWN";

    private final IAuthLogService authLogService;

    private final UserDao userDao;

    private final RoleDao roleDao;

    public void recordRoleGrant(String tenantId, String orgId, String roleCode, String authDescribe) {
        AuthLogDO authLogDO = baseLog(tenantId, orgId);
        fillOperator(authLogDO);
        fillRoleTarget(authLogDO, tenantId, orgId, roleCode);
        authLogDO.setAuthDescribe(authDescribe);
        authLogService.saveLog(authLogDO);
    }

    public void recordUserRoleGrant(String tenantId, String orgId, String userCode, String authDescribe) {
        AuthLogDO authLogDO = baseLog(tenantId, orgId);
        fillOperator(authLogDO);
        fillUserTarget(authLogDO, userCode);
        authLogDO.setAuthDescribe(authDescribe);
        authLogService.saveLog(authLogDO);
    }

    private AuthLogDO baseLog(String tenantId, String orgId) {
        AuthLogDO authLogDO = new AuthLogDO();
        authLogDO.setTenantId(tenantId);
        authLogDO.setOrgId(orgId);
        return authLogDO;
    }

    private void fillOperator(AuthLogDO authLogDO) {
        OperatorSnapshot operator = resolveOperator();
        authLogDO.setOperatorUserId(operator.userId());
        authLogDO.setOperatorCode(operator.userCode());
        authLogDO.setOperatorName(operator.userName());
    }

    private void fillRoleTarget(AuthLogDO authLogDO, String tenantId, String orgId, String roleCode) {
        String safeRoleCode = StringUtil.isBlank(roleCode) ? UNKNOWN : roleCode.trim();
        authLogDO.setUserId(safeRoleCode);
        authLogDO.setUserCode(safeRoleCode);
        RoleDO query = new RoleDO();
        query.setTenantId(tenantId);
        query.setOrgId(orgId);
        query.setRoleCode(safeRoleCode);
        var roles = roleDao.select(query);
        if (roles != null && !roles.isEmpty() && StringUtil.isNotBlank(roles.get(0).getRoleName())) {
            authLogDO.setUserName(roles.get(0).getRoleName());
        } else {
            authLogDO.setUserName(safeRoleCode);
        }
    }

    private void fillUserTarget(AuthLogDO authLogDO, String userCode) {
        String safeUserCode = StringUtil.isBlank(userCode) ? UNKNOWN : userCode.trim();
        UserDO query = new UserDO();
        query.setUserCode(safeUserCode);
        var users = userDao.select(query);
        if (users != null && !users.isEmpty()) {
            UserDO user = users.get(0);
            authLogDO.setUserId(StringUtil.isBlank(user.getUserId()) ? safeUserCode : user.getUserId());
            authLogDO.setUserCode(StringUtil.isBlank(user.getUserCode()) ? safeUserCode : user.getUserCode());
            authLogDO.setUserName(StringUtil.isBlank(user.getUserName()) ? safeUserCode : user.getUserName());
            return;
        }
        authLogDO.setUserId(safeUserCode);
        authLogDO.setUserCode(safeUserCode);
        authLogDO.setUserName(safeUserCode);
    }

    private OperatorSnapshot resolveOperator() {
        try {
            String userId = StpUtil.getLoginIdAsString();
            if (StringUtil.isBlank(userId)) {
                return OperatorSnapshot.unknown();
            }
            UserDO user = userDao.selectById(userId);
            if (user == null) {
                return new OperatorSnapshot(userId, userId, userId);
            }
            return new OperatorSnapshot(
                    user.getUserId(),
                    StringUtil.isBlank(user.getUserCode()) ? user.getUserId() : user.getUserCode(),
                    StringUtil.isBlank(user.getUserName()) ? user.getUserCode() : user.getUserName());
        } catch (RuntimeException exception) {
            return OperatorSnapshot.unknown();
        }
    }

    private record OperatorSnapshot(String userId, String userCode, String userName) {

        static OperatorSnapshot unknown() {
            return new OperatorSnapshot(UNKNOWN, UNKNOWN, UNKNOWN);
        }
    }
}
