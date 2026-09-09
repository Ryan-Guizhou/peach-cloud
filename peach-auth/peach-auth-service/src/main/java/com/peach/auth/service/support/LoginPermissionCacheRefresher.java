package com.peach.auth.service.support;

import com.peach.auth.dao.AuthPartyDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dao.UserOrgDao;
import com.peach.auth.entity.AuthPartyDO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.service.IRoleService;
import com.peach.auth.service.assembler.LoginPermissionAssembler;
import com.peach.auth.vo.LoginPermissionSnapshotVO;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.UserOrgVO;
import com.peach.auth.vo.UserVO;
import com.peach.common.util.StringUtil;
import com.peach.satoken.constant.SatokenConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 登录权限缓存刷新：授权变更后同步网关共享读模型。
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class LoginPermissionCacheRefresher {

    private final StringRedisTemplate stringRedisTemplate;

    private final UserDao userDao;

    private final UserOrgDao userOrgDao;

    private final IRoleService roleService;

    private final LoginPermissionAssembler loginPermissionAssembler;

    private final AuthPartyDao authPartyDao;

    public void refreshUsersByRole(String tenantId, String orgId, String roleCode, Integer fiscal) {
        if (StringUtil.isBlank(tenantId) || StringUtil.isBlank(orgId) || StringUtil.isBlank(roleCode)) {
            return;
        }
        AuthPartyDO query = new AuthPartyDO();
        query.setTenantId(tenantId);
        query.setOrgId(orgId);
        query.setRoleCode(roleCode);
        query.setFiscal(fiscal);
        List<String> userCodes = authPartyDao.selectUserCodesByRole(query);
        if (CollectionUtils.isEmpty(userCodes)) {
            return;
        }
        for (String userCode : userCodes) {
            refreshUserByCode(tenantId, orgId, userCode, fiscal);
        }
    }

    public void refreshUserByCode(String tenantId, String orgId, String userCode, Integer fiscal) {
        if (StringUtil.isBlank(tenantId) || StringUtil.isBlank(orgId) || StringUtil.isBlank(userCode)) {
            return;
        }
        UserDO query = new UserDO();
        query.setUserCode(userCode.trim());
        List<UserVO> users = userDao.select(query);
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        UserVO userVO = users.get(0);
        UserSensitiveFieldSupport.decryptUserFields(userVO);
        UserOrgVO currentUserOrg = resolveUserOrg(userVO.getUserId(), tenantId, orgId);
        if (currentUserOrg == null) {
            return;
        }
        RoleQO roleQO = new RoleQO();
        roleQO.setTenantId(tenantId);
        roleQO.setOrgId(orgId);
        roleQO.setUserCode(userCode.trim());
        roleQO.setFiscal(fiscal);
        List<RoleVO> roleList = roleService.selectByUserCode(roleQO);
        LoginPermissionSnapshotVO snapshot = loginPermissionAssembler.assemble(
                currentUserOrg, fiscal, roleList);
        writeCache(userVO, currentUserOrg, fiscal, snapshot);
    }

    public void writeCache(UserVO userVO, UserOrgVO currentUserOrg, Integer fiscal,
                           LoginPermissionSnapshotVO snapshot) {
        String userId = userVO.getUserId();
        String profileKey = SatokenConstant.USER_PROFILE_CACHE_PREFIX + userId;
        String apiKey = "peach:security:user:api-resources:" + userId;
        String buttonKey = "peach:security:user:button-resources:" + userId;
        stringRedisTemplate.delete(profileKey);
        stringRedisTemplate.delete(apiKey);
        stringRedisTemplate.delete(buttonKey);

        Map<String, String> profile = new LinkedHashMap<>();
        profile.put(SatokenConstant.USER_PROFILE_FIELD_USER_ID, userId);
        profile.put(SatokenConstant.USER_PROFILE_FIELD_USER_CODE, userVO.getUserCode());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_USER_NAME, userVO.getUserName());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_TENANT_ID, currentUserOrg.getTenantId());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_TENANT_NAME, currentUserOrg.getTenantName());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_ORG_ID, currentUserOrg.getOrgId());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_ORG_CODE, currentUserOrg.getOrgCode());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_ORG_NAME, currentUserOrg.getOrgName());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_FISCAL, fiscal == null ? "" : String.valueOf(fiscal));
        profile.put(SatokenConstant.USER_PROFILE_FIELD_CONTEXT_VERSION, "1");
        stringRedisTemplate.opsForHash().putAll(profileKey, profile);

        Set<String> apiResources = snapshot.getApiResourceCodes() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(snapshot.getApiResourceCodes());
        Set<String> buttonResources = snapshot.getButtonResourceCodes() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(snapshot.getButtonResourceCodes());
        if (!apiResources.isEmpty()) {
            stringRedisTemplate.opsForSet().add(apiKey, apiResources.toArray(new String[0]));
        }
        if (!buttonResources.isEmpty()) {
            stringRedisTemplate.opsForSet().add(buttonKey, buttonResources.toArray(new String[0]));
        }
        log.debug("Refreshed login permission cache, userId={}, tenantId={}, orgId={}",
                userId, currentUserOrg.getTenantId(), currentUserOrg.getOrgId());
    }

    private UserOrgVO resolveUserOrg(String userId, String tenantId, String orgId) {
        List<UserOrgVO> userOrgList = userOrgDao.selectByUserIdAndTenantId(userId, tenantId);
        if (CollectionUtils.isEmpty(userOrgList)) {
            return null;
        }
        for (UserOrgVO userOrgVO : userOrgList) {
            if (userOrgVO != null && orgId.equals(userOrgVO.getOrgId())) {
                return userOrgVO;
            }
        }
        return userOrgList.get(0);
    }
}
