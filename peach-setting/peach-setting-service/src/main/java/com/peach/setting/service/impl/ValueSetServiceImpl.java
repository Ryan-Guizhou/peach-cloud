package com.peach.setting.service.impl;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.PageResult;
import com.peach.common.util.DateUtil;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.setting.comon.enums.SettingConst;
import com.peach.setting.dao.ValueSetDao;
import com.peach.setting.dao.ValueSetItemDao;
import com.peach.setting.dto.ValueSetDTO;
import com.peach.setting.dto.ValueSetItemDTO;
import com.peach.setting.entity.ValueSetDO;
import com.peach.setting.entity.ValueSetItemDO;
import com.peach.setting.qo.ValueSetItemQO;
import com.peach.setting.qo.ValueSetQO;
import com.peach.setting.service.IValueSetService;
import com.peach.setting.vo.ValueSetItemVO;
import com.peach.setting.vo.ValueSetVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 值集服务实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 值集服务实现
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ValueSetServiceImpl implements IValueSetService {

        private final ValueSetDao valueSetDao;

        private final ValueSetItemDao valueSetItemDao;

    @Override
    public PageResult<ValueSetVO> pageList(ValueSetQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<ValueSetVO> pageInfo = PageMethod.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> valueSetDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_VALUE_SET, cacheResolver = "settingCacheResolver", key = "'id:' + #id", unless = "#result == null")
    public ValueSetVO selectById(String id) {
        return valueSetDao.selectById(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET, cacheResolver = "settingCacheResolver", allEntries = true),
            @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void save(ValueSetDTO data) {
        ValueSetDO valueSetDO = new ValueSetDO();
        BeanUtils.copyProperties(data, valueSetDO);
        valueSetDO.setId(IDGeneratorUtil.generateUuid());
        valueSetDO.fillCreateTime();
        valueSetDao.insert(valueSetDO);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET, cacheResolver = "settingCacheResolver", allEntries = true),
            @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void update(ValueSetDTO data) {
        ValueSetDO valueSetDO = new ValueSetDO();
        BeanUtils.copyProperties(data, valueSetDO);
        valueSetDO.setModifyTime(DateUtil.nowTime());
        valueSetDao.updateById(valueSetDO);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET, cacheResolver = "settingCacheResolver", allEntries = true),
            @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<String> ids) {
        for (String id : ids) {
            ValueSetVO valueSet = valueSetDao.selectById(id);
            if (valueSet != null && valueSet.getValueSetCode() != null) {
                valueSetItemDao.delByValueSetCode(valueSet.getValueSetCode());
            }
        }
        valueSetDao.delByIds(ids);
    }

    @Override
    public PageResult<ValueSetItemVO> itemPageList(ValueSetItemQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<ValueSetItemVO> pageInfo = PageMethod.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> valueSetItemDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_VALUE_SET_ITEM, cacheResolver = "settingCacheResolver", key = "'code:' + #valueSetCode", unless = "#result == null")
    public List<ValueSetItemVO> itemListByValueSetCode(String valueSetCode) {
        return valueSetItemDao.selectByValueSetCode(valueSetCode);
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_VALUE_SET_ITEM, cacheResolver = "settingCacheResolver", key = "'id:' + #id", unless = "#result == null")
    public ValueSetItemVO itemSelectById(String id) {
        return valueSetItemDao.selectById(id);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void saveItem(ValueSetItemDTO data) {
        ValueSetItemDO valueSetItemDO = new ValueSetItemDO();
        BeanUtils.copyProperties(data, valueSetItemDO);
        valueSetItemDO.setId(IDGeneratorUtil.generateUuid());
        valueSetItemDO.fillCreateTime();
        valueSetItemDao.insert(valueSetItemDO);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(ValueSetItemDTO data) {
        ValueSetItemDO valueSetItemDO = new ValueSetItemDO();
        BeanUtils.copyProperties(data, valueSetItemDO);
        valueSetItemDO.setModifyTime(DateUtil.nowTime());
        valueSetItemDao.updateById(valueSetItemDO);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_VALUE_SET_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(List<String> ids) {
        valueSetItemDao.delByIds(ids);
    }

    private void fillCurrentTenantOrg(ValueSetQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private void fillCurrentTenantOrg(ValueSetItemQO qo) {
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

