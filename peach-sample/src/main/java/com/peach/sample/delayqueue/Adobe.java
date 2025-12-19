package com.peach.sample.delayqueue;

import com.peach.redission.delayqueue.core.ConsumerTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 18:54
 */
@Slf4j
@Component
public class Adobe implements ConsumerTask {

    @Override
    public void execute(String content) {
      log.info("消费消息:[{}]",content);
    }

    @Override
    public String topic() {
        return "delay-demo-queue";
    }
}
