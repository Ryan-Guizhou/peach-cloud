package com.peach.setting.service.impl;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.PageResult;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.DateUtil;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.setting.comon.enums.SettingConst;
import com.peach.setting.dao.IpWhitelistDao;
import com.peach.setting.dto.IpWhitelistDTO;
import com.peach.setting.entity.IpWhitelistDO;
import com.peach.setting.qo.IpWhitelistQO;
import com.peach.setting.service.IIpWhitelistService;
import com.peach.setting.vo.IpWhitelistVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

/**
 * IP 白名单服务实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class IpWhitelistServiceImpl implements IIpWhitelistService {

        private final IpWhitelistDao ipWhitelistDao;

        private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<IpWhitelistVO> pageList(IpWhitelistQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<IpWhitelistVO> pageInfo = PageMethod.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> ipWhitelistDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    public IpWhitelistVO selectById(String id) {
        return ipWhitelistDao.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(IpWhitelistDTO data) {
        IpWhitelistDO ipWhitelistDO = new IpWhitelistDO();
        BeanUtils.copyProperties(data, ipWhitelistDO);
        ipWhitelistDO.setId(IDGeneratorUtil.generateUuid());
        ipWhitelistDO.fillCreateTime();
        if (ipWhitelistDO.getStatus() == null) {
            ipWhitelistDO.setStatus(PubCommonConst.LOGIC_TRUE);
        }
        ipWhitelistDao.insert(ipWhitelistDO);
        refreshCacheAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(IpWhitelistDTO data) {
        IpWhitelistDO ipWhitelistDO = new IpWhitelistDO();
        BeanUtils.copyProperties(data, ipWhitelistDO);
        ipWhitelistDO.setModifyTime(DateUtil.nowTime());
        ipWhitelistDao.updateById(ipWhitelistDO);
        refreshCacheAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<String> ids) {
        ipWhitelistDao.delByIds(ids);
        refreshCacheAfterCommit();
    }

    @Override
    public void warmUpCache() {
        refreshCache();
    }

    /**
     * 刷新网关 IP 白名单 Redis Set。
     */
    private void refreshCache() {
        List<IpWhitelistVO> enabledList = ipWhitelistDao.selectEnabledList();
        if (enabledList == null || enabledList.isEmpty()) {
            stringRedisTemplate.delete(SettingConst.GATEWAY_IP_WHITELIST_KEY);
            return;
        }
        String tempKey = SettingConst.GATEWAY_IP_WHITELIST_KEY + ":tmp:" + UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.delete(tempKey);
        boolean hasValue = false;
        for (IpWhitelistVO item : enabledList) {
            if (item.getIpAddress() != null && !item.getIpAddress().isBlank()) {
                stringRedisTemplate.opsForSet().add(tempKey, item.getIpAddress().trim());
                hasValue = true;
            }
        }
        if (hasValue) {
            stringRedisTemplate.rename(tempKey, SettingConst.GATEWAY_IP_WHITELIST_KEY);
        } else {
            stringRedisTemplate.delete(SettingConst.GATEWAY_IP_WHITELIST_KEY);
        }
    }

    /**
     * 在事务提交后刷新缓存；不存在事务时直接刷新。
     */
    private void refreshCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refreshCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshCache();
            }
        });
    }

    private void fillCurrentTenantOrg(IpWhitelistQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private String requireTenantId() {
        String tenantId = SecurityContextHolder.currentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Current tenant context is missing");
        }
        return tenantId;
    }

    private String requireOrgId() {
        String orgId = SecurityContextHolder.currentOrgId();
        if (orgId == null || orgId.isBlank()) {
            throw new IllegalStateException("Current organization context is missing");
        }
        return orgId;
    }
}
