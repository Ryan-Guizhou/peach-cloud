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
import com.peach.setting.dao.LanguageDao;
import com.peach.setting.dao.MultiMessageDao;
import com.peach.setting.dto.LanguageDTO;
import com.peach.setting.dto.MultiMessageDTO;
import com.peach.setting.entity.LanguageDO;
import com.peach.setting.entity.MultiMessageDO;
import com.peach.setting.qo.LanguageQO;
import com.peach.setting.qo.MulitMessageQO;
import com.peach.setting.service.IMultiMessageService;
import com.peach.setting.vo.LanguageVO;
import com.peach.setting.vo.MulitMessageVO;
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
 * 多语言服务实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 多语言服务实现
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class MultiMessageServiceImpl implements IMultiMessageService {

        private final LanguageDao languageDao;

        private final MultiMessageDao multiMessageDao;

    @Override
    public PageResult<LanguageVO> languagePageList(LanguageQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<LanguageVO> pageInfo = PageMethod.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> languageDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_LANGUAGE, cacheResolver = "settingCacheResolver", key = "'id:' + #id", unless = "#result == null")
    public LanguageVO languageSelectById(String id) {
        return languageDao.selectById(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_LANGUAGE, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void saveLanguage(LanguageDTO data) {
        LanguageDO languageDO = new LanguageDO();
        BeanUtils.copyProperties(data, languageDO);
        if (PubCommonConst.LOGIC_TRUE.equals(languageDO.getDefaultFlag())) {
            languageDao.clearDefaultFlag();
        }
        languageDO.setId(IDGeneratorUtil.generateUuid());
        languageDO.fillCreateTime();
        languageDao.insert(languageDO);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_LANGUAGE, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void updateLanguage(LanguageDTO data) {
        LanguageDO languageDO = new LanguageDO();
        BeanUtils.copyProperties(data, languageDO);
        if (PubCommonConst.LOGIC_TRUE.equals(languageDO.getDefaultFlag())) {
            languageDao.clearDefaultFlag();
        }
        languageDO.setModifyTime(DateUtil.nowTime());
        languageDao.updateById(languageDO);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = SettingConst.CACHE_LANGUAGE, cacheResolver = "settingCacheResolver", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void deleteLanguage(List<String> ids) {
        languageDao.delByIds(ids);
    }

    @Override
    public PageResult<MulitMessageVO> messagePageList(MulitMessageQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<MulitMessageVO> pageInfo = PageMethod.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> multiMessageDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_MESSAGE, cacheResolver = "settingCacheResolver", key = "'key:' + #messageKey", unless = "#result == null")
    public List<MulitMessageVO> messageListByKey(String messageKey) {
        return multiMessageDao.selectByMessageKey(messageKey);
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_MESSAGE, cacheResolver = "settingCacheResolver", key = "'id:' + #id", unless = "#result == null")
    public MulitMessageVO messageSelectById(String id) {
        return multiMessageDao.selectById(id);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_MESSAGE, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(MultiMessageDTO data) {
        MultiMessageDO multiMessageDO = new MultiMessageDO();
        BeanUtils.copyProperties(data, multiMessageDO);
        multiMessageDO.setId(IDGeneratorUtil.generateUuid());
        multiMessageDO.fillCreateTime();
        multiMessageDao.insert(multiMessageDO);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_MESSAGE, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updateMessage(MultiMessageDTO data) {
        MultiMessageDO multiMessageDO = new MultiMessageDO();
        BeanUtils.copyProperties(data, multiMessageDO);
        multiMessageDO.setModifyTime(DateUtil.nowTime());
        multiMessageDao.updateById(multiMessageDO);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_MESSAGE, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(List<String> ids) {
        multiMessageDao.delByIds(ids);
    }

    private void fillCurrentTenantOrg(LanguageQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private void fillCurrentTenantOrg(MulitMessageQO qo) {
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

