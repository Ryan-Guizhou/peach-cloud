package com.peach.auth.service.impl;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dao.AuthLogDao;
import com.peach.auth.entity.AuthLogDO;
import com.peach.auth.qo.AuthLogQO;
import com.peach.auth.service.IAuthLogService;
import com.peach.auth.vo.AuthLogVO;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 认证日志服务实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:27
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class AuthLogServiceImpl implements IAuthLogService {

        private final AuthLogDao authLogDao;

    @Override
    public PageInfo<AuthLogVO> pageList(AuthLogQO authLogQO) {
        return PageMethod.startPage(authLogQO.getPageNum(), authLogQO.getPageSize())
                .doSelectPageInfo(() -> authLogDao.select(buildQuery(authLogQO)));
    }

    @Override
    public void saveLog(AuthLogDO authLogDO) {
        if (authLogDO == null) {
            return;
        }
        if (StringUtil.isBlank(authLogDO.getLogId())) {
            authLogDO.setLogId(IDGeneratorUtil.generateUuid());
        }
        if (StringUtil.isBlank(authLogDO.getOperatTime())) {
            authLogDO.setOperatTime(DateUtil.nowTime());
        }
        authLogDao.insert(authLogDO);
    }

    private AuthLogDO buildQuery(AuthLogQO authLogQO) {
        AuthLogDO authLogDO = new AuthLogDO();
        if (authLogQO == null) {
            return authLogDO;
        }
        authLogDO.setTenantId(authLogQO.getTenantId());
        authLogDO.setOrgId(authLogQO.getOrgId());
        authLogDO.setOperatorUserId(authLogQO.getOperatorUserId());
        authLogDO.setUserId(authLogQO.getUserId());
        return authLogDO;
    }
}
