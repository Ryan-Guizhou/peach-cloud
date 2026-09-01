package com.peach.setting.service.impl;

import lombok.RequiredArgsConstructor;

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

import java.util.HashMap;
import java.util.Map;

/**
 * 公告阅读记录定时落库任务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 公告阅读记录定时落库任务
 */
@Slf4j
@Indexed
@Component
@EnableScheduling
@RequiredArgsConstructor
public class NoticeReadFlushTask {

    private final RedisDao redisDao;

    private final NoticeReadRecordDao noticeReadRecordDao;

    private final NoticeDao noticeDao;

    private final MultiCacheManagerService multiCacheManagerService;

    @Scheduled(fixedRate = 15000)
    public void flushNoticeReadRecord() {
        long size = redisDao.lSize(SettingConst.NOTICE_READ_PENDING_LIST);
        if (size <= 0) {
            return;
        }
        Map<String, Integer> readCountMap = new HashMap<>();
        for (long i = 0; i < size; i++) {
            processPendingReadRecord(readCountMap);
        }
        applyReadCountUpdates(readCountMap);
    }

    private void processPendingReadRecord(Map<String, Integer> readCountMap) {
        Object item = redisDao.lLeftPop(SettingConst.NOTICE_READ_PENDING_LIST);
        if (item == null) {
            return;
        }
        String pendingKey = String.valueOf(item);
        Object value = redisDao.vGet(pendingKey);
        redisDao.delete(pendingKey);
        if (value == null) {
            return;
        }
        String suffix = pendingKey.substring(SettingConst.NOTICE_READ_PENDING_KEY_PREFIX.length());
        String[] arr = suffix.split(":", 2);
        if (arr.length != 2) {
            return;
        }
        persistReadRecordIfAbsent(arr[0], arr[1], String.valueOf(value), readCountMap);
    }

    private void persistReadRecordIfAbsent(String noticeCode, String readUserId, String readTime,
                                           Map<String, Integer> readCountMap) {
        NoticeReadRecordVO exists = noticeReadRecordDao.selectByNoticeCodeAndUserId(noticeCode, readUserId);
        if (exists != null) {
            return;
        }
        NoticeVO notice = noticeDao.selectByNoticeCode(noticeCode);
        if (notice == null) {
            return;
        }
        if (notice.getTenantId() == null || notice.getTenantId().isBlank()
                || notice.getOrgId() == null || notice.getOrgId().isBlank()) {
            log.warn("skip notice read record flush because notice tenant or organization is missing, noticeCode={}", noticeCode);
            return;
        }
        NoticeReadRecordDO readRecord = new NoticeReadRecordDO();
        readRecord.setId(IDGeneratorUtil.generateUuid());
        readRecord.setNoticeCode(noticeCode);
        readRecord.setTenantId(notice.getTenantId());
        readRecord.setOrgId(notice.getOrgId());
        readRecord.setReadUserId(readUserId);
        readRecord.setReadTime(readTime);
        readRecord.setCreatedTime(DateUtil.nowTime());
        noticeReadRecordDao.insert(readRecord);
        readCountMap.merge(noticeCode, 1, Integer::sum);
    }

    private void applyReadCountUpdates(Map<String, Integer> readCountMap) {
        for (Map.Entry<String, Integer> entry : readCountMap.entrySet()) {
            noticeDao.increaseReadCount(entry.getKey(), entry.getValue());
        }
        if (!readCountMap.isEmpty()) {
            multiCacheManagerService.clear(SettingConst.CACHE_NOTICE);
            log.info("flushed notice read records: {}", readCountMap.size());
        }
    }
}
