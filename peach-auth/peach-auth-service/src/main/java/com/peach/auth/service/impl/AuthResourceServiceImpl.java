package com.peach.auth.service.impl;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import cn.dev33.satoken.stp.StpUtil;
import com.github.pagehelper.PageInfo;
import com.peach.auth.dao.AuthResourceDao;
import com.peach.auth.dto.RoleResourceAuthDTO;
import com.peach.auth.entity.AuthLogDO;
import com.peach.auth.entity.AuthResourceDO;
import com.peach.auth.qo.AuthResourceQO;
import com.peach.auth.service.IAuthLogService;
import com.peach.auth.service.IAuthResourceService;
import com.peach.auth.vo.AuthResourceVO;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 授权资源服务实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:27
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class AuthResourceServiceImpl implements IAuthResourceService {

        private final AuthResourceDao authResourceDao;

        private final IAuthLogService authLogService;

    @Override
    public PageInfo<AuthResourceVO> pageList(AuthResourceQO authResourceQO) {
        return PageMethod.startPage(authResourceQO.getPageNum(), authResourceQO.getPageSize())
                .doSelectPageInfo(() -> authResourceDao.select(buildQuery(authResourceQO)));
    }

    @Override
    public List<AuthResourceVO> list(AuthResourceQO authResourceQO) {
        return authResourceDao.select(buildQuery(authResourceQO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleResources(RoleResourceAuthDTO roleResourceAuthDTO) {
        AuthResourceDO deleteQuery = new AuthResourceDO();
        deleteQuery.setTenantId(roleResourceAuthDTO.getTenantId());
        deleteQuery.setOrgId(roleResourceAuthDTO.getOrgId());
        deleteQuery.setPartyCode(roleResourceAuthDTO.getPartyCode());
        deleteQuery.setFiscal(roleResourceAuthDTO.getFiscal());
        authResourceDao.del(deleteQuery);

        List<AuthResourceDO> resourceList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(roleResourceAuthDTO.getResourceList())) {
            for (RoleResourceAuthDTO.RoleResourceItemDTO itemDTO : roleResourceAuthDTO.getResourceList()) {
                AuthResourceDO authResourceDO = new AuthResourceDO();
                authResourceDO.setResourceId(IDGeneratorUtil.generateUuid());
                authResourceDO.setTenantId(roleResourceAuthDTO.getTenantId());
                authResourceDO.setOrgId(roleResourceAuthDTO.getOrgId());
                authResourceDO.setPartyCode(roleResourceAuthDTO.getPartyCode());
                authResourceDO.setFuncCode(itemDTO.getFuncCode());
                authResourceDO.setOpType(itemDTO.getOpType());
                authResourceDO.setResourceCode(itemDTO.getResourceCode());
                authResourceDO.setResourceName(itemDTO.getResourceName());
                authResourceDO.setAppId(roleResourceAuthDTO.getAppId());
                authResourceDO.setFiscal(roleResourceAuthDTO.getFiscal());
                authResourceDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
                authResourceDO.fillCreateTime(currentOperator());
                resourceList.add(authResourceDO);
            }
        }
        if (!resourceList.isEmpty()) {
            authResourceDao.batchInsert(resourceList);
        }
        recordAuthLog(roleResourceAuthDTO, resourceList.size());
    }

    private AuthResourceDO buildQuery(AuthResourceQO authResourceQO) {
        AuthResourceDO authResourceDO = new AuthResourceDO();
        if (authResourceQO == null) {
            return authResourceDO;
        }
        authResourceDO.setTenantId(authResourceQO.getTenantId());
        authResourceDO.setOrgId(authResourceQO.getOrgId());
        authResourceDO.setPartyCode(authResourceQO.getPartyCode());
        authResourceDO.setFuncCode(authResourceQO.getFuncCode());
        authResourceDO.setOpType(authResourceQO.getOpType());
        authResourceDO.setResourceCode(authResourceQO.getResourceCode());
        authResourceDO.setAppId(authResourceQO.getAppId());
        authResourceDO.setFiscal(authResourceQO.getFiscal());
        authResourceDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        return authResourceDO;
    }

    private void recordAuthLog(RoleResourceAuthDTO roleResourceAuthDTO, int resourceCount) {
        AuthLogDO authLogDO = new AuthLogDO();
        authLogDO.setTenantId(roleResourceAuthDTO.getTenantId());
        authLogDO.setOrgId(roleResourceAuthDTO.getOrgId());
        authLogDO.setOperatorUserId(currentOperator());
        authLogDO.setUserCode(roleResourceAuthDTO.getPartyCode());
        authLogDO.setAuthDescribe("角色资源授权，角色编码：" + roleResourceAuthDTO.getPartyCode()
                + "，资源数量：" + resourceCount);
        authLogDO.setOperatTime(DateUtil.nowTime());
        authLogService.saveLog(authLogDO);
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
