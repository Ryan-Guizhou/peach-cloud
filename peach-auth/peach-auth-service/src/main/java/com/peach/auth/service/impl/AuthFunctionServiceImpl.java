package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.auth.dao.AuthFunctionDao;
import com.peach.auth.dto.RoleFunctionAuthDTO;
import com.peach.auth.entity.AuthFunctionDO;
import com.peach.auth.entity.AuthLogDO;
import com.peach.auth.qo.AuthFunctionQO;
import com.peach.auth.service.IAuthFunctionService;
import com.peach.auth.service.IAuthLogService;
import com.peach.auth.vo.AuthFunctionVO;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 授权功能服务实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:27
 */
@Slf4j
@Indexed
@Service
public class AuthFunctionServiceImpl implements IAuthFunctionService {

    @Resource
    private AuthFunctionDao authFunctionDao;

    @Resource
    private IAuthLogService authLogService;

    @Override
    public PageInfo<AuthFunctionVO> pageList(AuthFunctionQO authFunctionQO) {
        return PageHelper.startPage(authFunctionQO.getPageNum(), authFunctionQO.getPageSize())
                .doSelectPageInfo(() -> authFunctionDao.select(buildQuery(authFunctionQO)));
    }

    @Override
    public List<AuthFunctionVO> list(AuthFunctionQO authFunctionQO) {
        return authFunctionDao.select(buildQuery(authFunctionQO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleFunctions(RoleFunctionAuthDTO roleFunctionAuthDTO) {
        AuthFunctionDO deleteQuery = new AuthFunctionDO();
        deleteQuery.setTenantId(roleFunctionAuthDTO.getTenantId());
        deleteQuery.setOrgId(roleFunctionAuthDTO.getOrgId());
        deleteQuery.setPartyCode(roleFunctionAuthDTO.getPartyCode());
        deleteQuery.setFiscal(roleFunctionAuthDTO.getFiscal());
        authFunctionDao.del(deleteQuery);

        List<AuthFunctionDO> functionList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(roleFunctionAuthDTO.getFuncCodeList())) {
            for (String funcCode : roleFunctionAuthDTO.getFuncCodeList()) {
                if (StringUtil.isBlank(funcCode)) {
                    continue;
                }
                AuthFunctionDO authFunctionDO = new AuthFunctionDO();
                authFunctionDO.setId(IDGeneratorUtil.UUID());
                authFunctionDO.setTenantId(roleFunctionAuthDTO.getTenantId());
                authFunctionDO.setOrgId(roleFunctionAuthDTO.getOrgId());
                authFunctionDO.setPartyCode(roleFunctionAuthDTO.getPartyCode());
                authFunctionDO.setPartyType("ROLE");
                authFunctionDO.setFuncCode(funcCode);
                authFunctionDO.setFiscal(roleFunctionAuthDTO.getFiscal());
                authFunctionDO.setState("ENABLE");
                authFunctionDO.setAppId(roleFunctionAuthDTO.getAppId());
                authFunctionDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
                authFunctionDO.fillCreateTime(currentOperator());
                functionList.add(authFunctionDO);
            }
        }
        if (!functionList.isEmpty()) {
            authFunctionDao.batchInsert(functionList);
        }
        recordAuthLog(roleFunctionAuthDTO, functionList.size());
    }

    private AuthFunctionDO buildQuery(AuthFunctionQO authFunctionQO) {
        AuthFunctionDO authFunctionDO = new AuthFunctionDO();
        if (authFunctionQO == null) {
            return authFunctionDO;
        }
        authFunctionDO.setTenantId(authFunctionQO.getTenantId());
        authFunctionDO.setOrgId(authFunctionQO.getOrgId());
        authFunctionDO.setPartyCode(authFunctionQO.getPartyCode());
        authFunctionDO.setPartyType(authFunctionQO.getPartyType());
        authFunctionDO.setFuncCode(authFunctionQO.getFuncCode());
        authFunctionDO.setFiscal(authFunctionQO.getFiscal());
        authFunctionDO.setState(authFunctionQO.getState());
        authFunctionDO.setAppId(authFunctionQO.getAppId());
        authFunctionDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        return authFunctionDO;
    }

    private void recordAuthLog(RoleFunctionAuthDTO roleFunctionAuthDTO, int functionCount) {
        AuthLogDO authLogDO = new AuthLogDO();
        authLogDO.setTenantId(roleFunctionAuthDTO.getTenantId());
        authLogDO.setOrgId(roleFunctionAuthDTO.getOrgId());
        authLogDO.setOperatorUserId(currentOperator());
        authLogDO.setUserCode(roleFunctionAuthDTO.getPartyCode());
        authLogDO.setAuthDescribe("角色功能授权，角色编码：" + roleFunctionAuthDTO.getPartyCode()
                + "，功能数量：" + functionCount);
        authLogDO.setOperatTime(DateUtil.nowTime());
        authLogService.record(authLogDO);
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
