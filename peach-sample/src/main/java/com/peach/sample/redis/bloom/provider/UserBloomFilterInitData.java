package com.peach.sample.redis.bloom.provider;

import com.peach.initialize.base.AbstractAppStartedEventHandler;
import com.peach.redis.bloom.core.BloomFilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/18 15:41
 */
@Slf4j
@Component
public class UserBloomFilterInitData extends AbstractAppStartedEventHandler {

    @Autowired
    private BloomFilterService bloomFilterService;

    @Override
    public Integer executeOrder() {
        return 3;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        List<Integer> userIds = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            userIds.add(i);
        }
        bloomFilterService.addAll("user", userIds);
        log.info("namespace->user"+ bloomFilterService.status("user"));

        List<String> roders = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            roders.add(String.valueOf(i));
        }
        bloomFilterService.addAll("order", roders);
        log.info("namespace->order"+ bloomFilterService.status("order"));

    }
}
