package com.peach.userservice.core;

import com.google.common.collect.Lists;
import com.peach.common.util.PeachCollectionUtil;
import com.peach.userservice.vo.UserOperLogVO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/3/15 0:27
 */
@Slf4j
public class UserOperLogQueue {

    private static final UserOperLogQueue INSTANCE = new UserOperLogQueue();

    private final BlockingQueue<UserOperLogVO> userOperLogQueue = new LinkedBlockingQueue<>();

    private UserOperLogQueue() {} // 私有构造函数

    public static UserOperLogQueue getInstance() {
        return INSTANCE;
    }
    
    /**
     * 添加数据到队列中
     * @param userOperLog
     */
    public void addOperLogQueue(UserOperLogVO userOperLog) {
        userOperLogQueue.add(userOperLog);
    }

    /**
     * 一次性弹出所有数据
     * @return
     */
    public List<UserOperLogVO> getAllUserOperLog() {
        List<UserOperLogVO> resultList = Lists.newArrayList();
        if (PeachCollectionUtil.isEmpty(userOperLogQueue)){
            return resultList;
        }
        userOperLogQueue.drainTo(resultList);
        return resultList;
    }
    
}
