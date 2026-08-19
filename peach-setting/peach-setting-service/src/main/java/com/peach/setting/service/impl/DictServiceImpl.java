package com.peach.setting.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.PageResult;
import com.peach.common.util.DateUtil;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.setting.comon.enums.SettingConst;
import com.peach.setting.dao.DictItemDao;
import com.peach.setting.dao.DictTypeDao;
import com.peach.setting.dto.DictItemDTO;
import com.peach.setting.dto.DictTypeDTO;
import com.peach.setting.entity.DictItemDO;
import com.peach.setting.entity.DictTypeDO;
import com.peach.setting.qo.DictItemQO;
import com.peach.setting.qo.DictTypeQO;
import com.peach.setting.service.IDictService;
import com.peach.setting.vo.DictItemVO;
import com.peach.setting.vo.DictTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 字典服务实现
 */
@Slf4j
@Indexed
@Service
public class DictServiceImpl implements IDictService {

    @Resource
    private DictTypeDao dictTypeDao;

    @Resource
    private DictItemDao dictItemDao;

    @Override
    public PageResult<DictTypeVO> typePageList(DictTypeQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<DictTypeVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> dictTypeDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_DICT_TYPE, cacheResolver = "settingCacheResolver", key = "'id:' + #id", unless = "#result == null")
    public DictTypeVO typeSelectById(String id) {
        return dictTypeDao.selectById(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_DICT_TYPE, cacheResolver = "settingCacheResolver", allEntries = true),
            @CacheEvict(cacheNames = SettingConst.CACHE_DICT_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void saveType(DictTypeDTO data) {
        DictTypeDO dictTypeDO = new DictTypeDO();
        BeanUtils.copyProperties(data, dictTypeDO);
        dictTypeDO.setId(IDGeneratorUtil.UUID());
        dictTypeDO.fillCreateTime();
        dictTypeDao.insert(dictTypeDO);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_DICT_TYPE, cacheResolver = "settingCacheResolver", allEntries = true),
            @CacheEvict(cacheNames = SettingConst.CACHE_DICT_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void updateType(DictTypeDTO data) {
        DictTypeDO dictTypeDO = new DictTypeDO();
        BeanUtils.copyProperties(data, dictTypeDO);
        dictTypeDO.setModifyTime(DateUtil.nowTime());
        dictTypeDao.updateById(dictTypeDO);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_DICT_TYPE, cacheResolver = "settingCacheResolver", allEntries = true),
            @CacheEvict(cacheNames = SettingConst.CACHE_DICT_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(List<String> ids) {
        for (String id : ids) {
            DictTypeVO type = dictTypeDao.selectById(id);
            if (type != null && type.getDictCode() != null) {
                dictItemDao.delByDictCode(type.getDictCode());
            }
        }
        dictTypeDao.delByIds(ids);
    }

    @Override
    public PageResult<DictItemVO> itemPageList(DictItemQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<DictItemVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> dictItemDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_DICT_ITEM, cacheResolver = "settingCacheResolver", key = "'code:' + #dictCode", unless = "#result == null")
    public List<DictItemVO> itemListByDictCode(String dictCode) {
        return dictItemDao.selectByDictCode(dictCode);
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_DICT_ITEM, cacheResolver = "settingCacheResolver", key = "'id:' + #id", unless = "#result == null")
    public DictItemVO itemSelectById(String id) {
        return dictItemDao.selectById(id);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_DICT_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void saveItem(DictItemDTO data) {
        DictItemDO dictItemDO = new DictItemDO();
        BeanUtils.copyProperties(data, dictItemDO);
        dictItemDO.setId(IDGeneratorUtil.UUID());
        dictItemDO.fillCreateTime();
        dictItemDao.insert(dictItemDO);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_DICT_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(DictItemDTO data) {
        DictItemDO dictItemDO = new DictItemDO();
        BeanUtils.copyProperties(data, dictItemDO);
        dictItemDO.setModifyTime(DateUtil.nowTime());
        dictItemDao.update(dictItemDO);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_DICT_ITEM, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(List<String> ids) {
        dictItemDao.delByIds(ids);
    }

    private void fillCurrentTenantOrg(DictTypeQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private void fillCurrentTenantOrg(DictItemQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private String requireTenantId() {
        String tenantId = SecurityContextHolder.currentTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalStateException("Current tenant context is missing");
        }
        return tenantId;
    }

    private String requireOrgId() {
        String orgId = SecurityContextHolder.currentOrgId();
        if (orgId == null || orgId.trim().isEmpty()) {
            throw new IllegalStateException("Current organization context is missing");
        }
        return orgId;
    }
}
