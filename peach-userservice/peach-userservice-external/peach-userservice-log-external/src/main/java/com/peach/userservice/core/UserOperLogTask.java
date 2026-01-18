package com.peach.userservice.core;

import com.peach.common.util.PeachCollectionUtil;
import com.peach.userservice.vo.UserOperLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 19:09
 */
@Slf4j
@Indexed
@Component
@EnableScheduling
public class UserOperLogTask {

    private final UserOperLogQueue userOperLogQueue = UserOperLogQueue.getInstance();

    @Resource
    private IUserOperLogService userOperLogService;

    @Scheduled(fixedRate = 10_000)
    public void excuted() {
        log.info("The scheduled task has started executing");
        handleUserOperLog();
        log.info("The scheduled task has been completed");
    }
    public void handleUserOperLog() {
        List<UserOperLogVO> allUserOperLog = userOperLogQueue.getAllUserOperLog();
        if (PeachCollectionUtil.isEmpty(allUserOperLog)) {
            log.warn("The data to be inserted for this scheduled task execution is empty");
            return;
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("UserOperLogTask->handleUserOperLog has been executed");
        userOperLogService.batchInsert(allUserOperLog);
        stopWatch.stop();
        log.info("UserOperLogTask has been executed in {}", stopWatch.getTotalTimeMillis());
        if (log.isDebugEnabled()){
            log.debug(stopWatch.prettyPrint());
        }
    }
}
