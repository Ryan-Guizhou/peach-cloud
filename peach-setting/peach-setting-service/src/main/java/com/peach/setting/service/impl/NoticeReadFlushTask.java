package com.peach.setting.service.impl;

import com.peach.common.IDGeneratorUtil;
import com.peach.common.util.DateUtil;
import com.peach.redis.common.tool.RedisDao;
import com.peach.redis.manager.MultiCacheManagerService;
import com.peach.setting.comon.enums.SettingConst;
import com.peach.setting.dao.NoticeDao;
import com.peach.setting.dao.NoticeReadRecordDao;
import com.peach.setting.entity.NoticeReadRecordDO;
import com.peach.setting.vo.NoticeVO;
import com.peach.setting.vo.NoticeReadRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 公告阅读记录定时落库任务
 */
@Slf4j
@Indexed
@Component
@EnableScheduling
public class NoticeReadFlushTask {

    @Resource
    private RedisDao redisDao;

    @Resource
    private NoticeReadRecordDao noticeReadRecordDao;

    @Resource
    private NoticeDao noticeDao;

    @Resource
    private MultiCacheManagerService multiCacheManagerService;

    @Scheduled(fixedRate = 15000)
    public void flushNoticeReadRecord() {
        long size = redisDao.lSize(SettingConst.NOTICE_READ_PENDING_LIST);
        if (size <= 0) {
            return;
        }
        Map<String, Integer> readCountMap = new HashMap<>();
        for (long i = 0; i < size; i++) {
            Object item = redisDao.lLeftPop(SettingConst.NOTICE_READ_PENDING_LIST);
            if (item == null) {
                continue;
            }
            String pendingKey = String.valueOf(item);
            Object value = redisDao.vGet(pendingKey);
            redisDao.delete(pendingKey);
            if (value == null) {
                continue;
            }
            String suffix = pendingKey.substring(SettingConst.NOTICE_READ_PENDING_KEY_PREFIX.length());
            String[] arr = suffix.split(":", 2);
            if (arr.length != 2) {
                continue;
            }
            String noticeCode = arr[0];
            String readUserId = arr[1];
            NoticeReadRecordVO exists = noticeReadRecordDao.selectByNoticeCodeAndUserId(noticeCode, readUserId);
            if (exists != null) {
                continue;
            }
            NoticeVO notice = noticeDao.selectByNoticeCode(noticeCode);
            if (notice == null) {
                continue;
            }
            if (notice.getTenantId() == null || notice.getTenantId().trim().isEmpty()
                    || notice.getOrgId() == null || notice.getOrgId().trim().isEmpty()) {
                log.warn("skip notice read record flush because notice tenant or organization is missing, noticeCode={}", noticeCode);
                continue;
            }
            NoticeReadRecordDO record = new NoticeReadRecordDO();
            record.setId(IDGeneratorUtil.UUID());
            record.setNoticeCode(noticeCode);
            record.setTenantId(notice.getTenantId());
            record.setOrgId(notice.getOrgId());
            record.setReadUserId(readUserId);
            record.setReadTime(String.valueOf(value));
            record.setCreatedTime(DateUtil.nowTime());
            noticeReadRecordDao.insert(record);
            readCountMap.merge(noticeCode, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : readCountMap.entrySet()) {
            noticeDao.increaseReadCount(entry.getKey(), entry.getValue());
        }
        if (!readCountMap.isEmpty()) {
            multiCacheManagerService.clear(SettingConst.CACHE_NOTICE);
            log.info("flushed notice read records: {}", readCountMap.size());
        }
    }
}
