package com.peach.rocket.quickstart;

import com.peach.rocket.annotation.MqEvent;
import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.quickstart.event.OrderCreatedEvent;
import com.peach.rocket.quickstart.event.OrderPaidEvent;
import com.peach.rocket.quickstart.runner.PeachRocketDemoRunner;
import com.peach.rocket.quickstart.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * 无外部 RocketMQ 依赖的订单发布行为与注解契约验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceBehaviorTest {

    @Mock
    private MqPublisher mqPublisher;

    @Test
    void shouldDeclareMqEventAnnotations() {
        MqEvent created = OrderCreatedEvent.class.getAnnotation(MqEvent.class);
        MqEvent paid = OrderPaidEvent.class.getAnnotation(MqEvent.class);

        assertThat(created).isNotNull();
        assertThat(created.topic()).isEqualTo("order");
        assertThat(created.tag()).isEqualTo("created");
        assertThat(paid).isNotNull();
        assertThat(paid.topic()).isEqualTo("order");
        assertThat(paid.tag()).isEqualTo("paid");
    }

    @Test
    void shouldPublishDemoMessagesThroughMqPublisher() {
        OrderService orderService = new OrderService(mqPublisher);
        orderService.publishDemoMessages();

        verify(mqPublisher).publish(any(OrderCreatedEvent.class));
        verify(mqPublisher).publishAsync(any(OrderCreatedEvent.class));
        verify(mqPublisher).publishDelay(any(OrderCreatedEvent.class), any());
        verify(mqPublisher).publishOrderly(any(OrderPaidEvent.class), anyString());
    }

    @Test
    void shouldAllowDisablingDemoRunner() {
        ConditionalOnProperty condition = PeachRocketDemoRunner.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(condition).isNotNull();
        assertThat(condition.prefix()).isEqualTo("quickstart.rocket.demo");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isTrue();
    }
}
