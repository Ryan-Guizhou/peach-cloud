package com.peach.sample.delayqueue;

import org.springframework.stereotype.Indexed;
import com.peach.redission.delayqueue.core.ConsumerTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adobe相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 18:54
 */
@Slf4j
@Indexed
@Component
public class Adobe implements ConsumerTask {

    @Override
    public void execute(String content) {
      log.info("Consumed delay queue message: [{}]",content);
    }

    @Override
    public String topic() {
        return "delay-demo-queue";
    }
}
