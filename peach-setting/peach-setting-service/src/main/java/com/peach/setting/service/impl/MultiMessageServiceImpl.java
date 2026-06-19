package com.peach.setting.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.PageResult;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.DateUtil;
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

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 多语言服务实现
 */
@Slf4j
@Indexed
@Service
public class MultiMessageServiceImpl implements IMultiMessageService {

    @Resource
    private LanguageDao languageDao;

    @Resource
    private MultiMessageDao multiMessageDao;

    @Override
    public PageResult<LanguageVO> languagePageList(LanguageQO qo) {
        PageInfo<LanguageVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
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
        languageDO.setId(IDGeneratorUtil.UUID());
        languageDO.setCreatedTime(DateUtil.nowTime());
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
        PageInfo<MulitMessageVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
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
        multiMessageDO.setId(IDGeneratorUtil.UUID());
        multiMessageDO.setCreatedTime(DateUtil.nowTime());
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
}

