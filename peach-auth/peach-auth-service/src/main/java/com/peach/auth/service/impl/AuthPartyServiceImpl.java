package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dao.AuthPartyDao;
import com.peach.auth.dto.UserRoleAuthDTO;
import com.peach.auth.entity.AuthPartyDO;
import com.peach.auth.qo.AuthPartyQO;
import com.peach.auth.service.IAuthPartyService;
import com.peach.auth.service.support.AuthLogSupport;
import com.peach.auth.service.support.LoginPermissionCacheRefresher;
import com.peach.auth.vo.AuthPartyVO;
import com.peach.common.unique.UniqueIdFacade;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户角色绑定服务实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 14:20
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class AuthPartyServiceImpl implements IAuthPartyService {

    private final AuthPartyDao authPartyDao;

    private final AuthLogSupport authLogSupport;

    private final LoginPermissionCacheRefresher loginPermissionCacheRefresher;

    @Override
    public List<AuthPartyVO> listUserRoles(AuthPartyQO authPartyQO) {
        AuthPartyDO query = buildQuery(authPartyQO);
        query.setPartyType("USER");
        List<AuthPartyVO> records = authPartyDao.select(query);
        return records == null ? List.of() : records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserRoles(UserRoleAuthDTO userRoleAuthDTO) {
        if (userRoleAuthDTO == null
                || StringUtil.isBlank(userRoleAuthDTO.getTenantId())
                || StringUtil.isBlank(userRoleAuthDTO.getOrgId())
                || StringUtil.isBlank(userRoleAuthDTO.getUserCode())
                || false) {
            throw new IllegalArgumentException("Invalid user role authorization context");
        }
        AuthPartyDO deleteQuery = new AuthPartyDO();
        deleteQuery.setTenantId(userRoleAuthDTO.getTenantId());
        deleteQuery.setOrgId(userRoleAuthDTO.getOrgId());
        deleteQuery.setPartyCode(userRoleAuthDTO.getUserCode());
        deleteQuery.setPartyType("USER");
        authPartyDao.del(deleteQuery);

        List<AuthPartyDO> bindings = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userRoleAuthDTO.getRoleCodeList())) {
            Set<String> normalizedRoleCodes = new LinkedHashSet<>();
            for (String roleCode : userRoleAuthDTO.getRoleCodeList()) {
                if (StringUtil.isNotBlank(roleCode)) {
                    normalizedRoleCodes.add(roleCode.trim());
                }
            }
            for (String roleCode : normalizedRoleCodes) {
                if (StringUtil.isBlank(roleCode)) {
                    continue;
                }
                AuthPartyDO authPartyDO = new AuthPartyDO();
                authPartyDO.setId(UniqueIdFacade.nextId());
                authPartyDO.setTenantId(userRoleAuthDTO.getTenantId());
                authPartyDO.setOrgId(userRoleAuthDTO.getOrgId());
                authPartyDO.setRoleCode(roleCode.trim());
                authPartyDO.setRoleType("CUSTOM");
                authPartyDO.setPartyCode(userRoleAuthDTO.getUserCode().trim());
                authPartyDO.setPartyType("USER");
                authPartyDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
                authPartyDO.fillCreateTime(currentOperator());
                bindings.add(authPartyDO);
            }
        }
        if (!bindings.isEmpty()) {
            authPartyDao.batchInsert(bindings);
        }

        authLogSupport.recordUserRoleGrant(
                userRoleAuthDTO.getTenantId(),
                userRoleAuthDTO.getOrgId(),
                userRoleAuthDTO.getUserCode(),
                "用户角色绑定，用户账号：" + userRoleAuthDTO.getUserCode()
                        + "，角色数量：" + bindings.size());

        loginPermissionCacheRefresher.refreshUserByCode(
                userRoleAuthDTO.getTenantId(),
                userRoleAuthDTO.getOrgId(),
                userRoleAuthDTO.getUserCode());
    }

    private AuthPartyDO buildQuery(AuthPartyQO authPartyQO) {
        AuthPartyDO authPartyDO = new AuthPartyDO();
        if (authPartyQO == null) {
            return authPartyDO;
        }
        authPartyDO.setTenantId(authPartyQO.getTenantId());
        authPartyDO.setOrgId(authPartyQO.getOrgId());
        authPartyDO.setPartyCode(authPartyQO.getPartyCode());
        authPartyDO.setPartyType(authPartyQO.getPartyType());
        authPartyDO.setRoleCode(authPartyQO.getRoleCode());
        authPartyDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        return authPartyDO;
    }

    private String currentOperator() {
        try {
            String userId = StpUtil.getLoginIdAsString();
            return StringUtil.isBlank(userId) ? null : userId;
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
