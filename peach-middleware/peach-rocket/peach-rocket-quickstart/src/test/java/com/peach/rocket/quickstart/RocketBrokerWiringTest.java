package com.peach.rocket.quickstart;

import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.producer.RocketMqPublisher;
import com.peach.rocket.quickstart.config.InProcessMqPublisher;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证真实 Broker 模式不会被进程内发布器覆盖，传输层用替身隔离外部服务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 18:30
 */
@SpringBootTest(properties = {
        "quickstart.rocket.demo.enabled=false",
        "peach.rocket.consumer.dynamic-register=false",
        "peach.rocket.transaction.enabled=false",
        "spring.autoconfigure.exclude=org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"
})
class RocketBrokerWiringTest {

    @MockitoBean
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldUseBrokerPublisherWithoutInProcessBeans() {
        assertThat(applicationContext.getBeansOfType(MqPublisher.class)).hasSize(1);
        assertThat(applicationContext.getBean(MqPublisher.class)).isInstanceOf(RocketMqPublisher.class);
        assertThat(applicationContext.getBeansOfType(InProcessMqPublisher.class)).isEmpty();
    }
}
