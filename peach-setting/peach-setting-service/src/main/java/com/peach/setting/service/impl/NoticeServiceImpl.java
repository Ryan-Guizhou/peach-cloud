package com.peach.setting.service.impl;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.PageResult;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.DateUtil;
import com.peach.common.util.PeachCollectionUtil;
import com.peach.redis.common.tool.RedisDao;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.setting.comon.enums.SettingConst;
import com.peach.setting.comon.enums.SettingEnum;
import com.peach.setting.dao.NoticeDao;
import com.peach.setting.dao.SiteMessageDao;
import com.peach.setting.dto.NoticeDTO;
import com.peach.setting.dto.NoticePublishDTO;
import com.peach.setting.entity.NoticeDO;
import com.peach.setting.entity.SiteMessageDO;
import com.peach.setting.qo.NoticeQO;
import com.peach.setting.qo.SiteMessageQO;
import com.peach.setting.service.INoticeService;
import com.peach.setting.vo.NoticeVO;
import com.peach.setting.vo.SiteMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 公告服务实现
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements INoticeService {

        private final NoticeDao noticeDao;

        private final SiteMessageDao siteMessageDao;

        private final RedisDao redisDao;

    @Override
    public PageResult<NoticeVO> noticePageList(NoticeQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<NoticeVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> noticeDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    @Cacheable(cacheNames = SettingConst.CACHE_NOTICE, cacheResolver = "settingCacheResolver", key = "'id:' + #id", unless = "#result == null")
    public NoticeVO noticeSelectById(String id) {
        return noticeDao.selectById(id);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_NOTICE, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void saveNotice(NoticeDTO data) {
        NoticeDO noticeDO = new NoticeDO();
        BeanUtils.copyProperties(data, noticeDO);
        noticeDO.setId(IDGeneratorUtil.UUID());
        noticeDO.setReadCount(Optional.ofNullable(noticeDO.getReadCount()).orElse(PubCommonConst.LOGIC_FLASE));
        noticeDO.setPublishStatus(Optional.ofNullable(noticeDO.getPublishStatus()).orElse(SettingEnum.PublishStatus.DRAFT.getCode()));
        noticeDO.setInboxEnabled(Optional.ofNullable(noticeDO.getInboxEnabled()).orElse(PubCommonConst.LOGIC_FLASE));
        noticeDO.fillCreateTime();
        noticeDao.insert(noticeDO);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_NOTICE, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(NoticeDTO data) {
        NoticeDO noticeDO = new NoticeDO();
        BeanUtils.copyProperties(data, noticeDO);
        noticeDO.setModifyTime(DateUtil.nowTime());
        noticeDao.updateById(noticeDO);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_NOTICE, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(List<String> ids) {
        noticeDao.delByIds(ids);
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_NOTICE, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void publishNotice(NoticePublishDTO data) {
        NoticeVO db = noticeDao.selectById(data.getId());
        if (db == null) {
            log.info("公告不存在, ID:{}", data.getId());
            return;
        }
        NoticeDO update = new NoticeDO();
        update.setId(db.getId());
        update.setPublishStatus(SettingEnum.PublishStatus.PUBLISHED.getCode());
        update.fillModifyTime("");
        noticeDao.updateById(update);
        if (!PubCommonConst.LOGIC_TRUE.equals(db.getInboxEnabled()) || PeachCollectionUtil.isEmpty(db.getReceiverIdList())) {
            log.info("无需同步站内信");
            return;
        }
        List<SiteMessageDO> list = new ArrayList<>();
        for (String receiverId : data.getReceiverIdList()) {
            SiteMessageDO message = new SiteMessageDO();
            message.setId(IDGeneratorUtil.UUID());
            message.setMessageCode(IDGeneratorUtil.UUID());
            message.setReceiverId(receiverId);
            message.setTitleMessageKey(db.getTitleMessageKey());
            message.setContentMessageKey(db.getContentMessageKey());
            message.setMessageType(SettingEnum.MessageType.ANNOUNCEMENT.getCode());
            message.setSourceType(SettingEnum.MessageSourceType.ANNOUNCEMENT.getCode());
            message.setSourceCode(db.getNoticeCode());
            message.setReadFlag(PubCommonConst.LOGIC_FLASE);
            message.setSendStatus(SettingEnum.SendStatus.SENT.getCode());
            message.fillCreateTime(db.getTenantId(), db.getOrgId());
            list.add(message);
        }
        if (!list.isEmpty()) {
            siteMessageDao.batchInsert(list);
        }
    }

    @Override
    @CacheEvict(cacheNames = SettingConst.CACHE_NOTICE, cacheResolver = "settingCacheResolver", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void revokeNotice(String id) {
        NoticeVO db = noticeDao.selectById(id);
        if (db == null) {
            log.info("公告不存在, 无法撤销, ID:{}", id);
            return;
        }
        NoticeDO update = new NoticeDO();
        update.setId(id);
        update.setPublishStatus(SettingEnum.PublishStatus.REVOKED.getCode());
        update.setModifyTime(DateUtil.nowTime());
        noticeDao.updateById(update);
        siteMessageDao.revokeBySourceCode(db.getNoticeCode());
    }

    @Override
    public void markNoticeRead(String noticeCode, String userId) {
        String pendingKey = MessageFormat.format(SettingConst.NOTICE_READ_PENDING_KEY_PREFIX,noticeCode,userId);
        boolean exists = redisDao.existsKey(pendingKey);
        redisDao.vSet(pendingKey, DateUtil.nowTime(), SettingConst.NOTICE_READ_PENDING_EXPIRE_SECONDS);
        if (!exists) {
            redisDao.lRightPush(SettingConst.NOTICE_READ_PENDING_LIST, pendingKey);
        }
    }

    @Override
    public PageResult<SiteMessageVO> siteMessagePageList(SiteMessageQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<SiteMessageVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> siteMessageDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSiteMessageRead(String id) {
        SiteMessageDO update = new SiteMessageDO();
        update.setId(id);
        update.setReadFlag(PubCommonConst.LOGIC_TRUE);
        update.setModifyTime(DateUtil.nowTime());
        siteMessageDao.updateById(update);
    }

    private void fillCurrentTenantOrg(NoticeQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private void fillCurrentTenantOrg(SiteMessageQO qo) {
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

