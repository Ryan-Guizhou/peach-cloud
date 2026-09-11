package com.peach.rocket.quickstart;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.annotation.MqEvent;
import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.quickstart.consumer.OrderCreatedConsumer;
import com.peach.rocket.quickstart.consumer.OrderPaidConsumer;
import com.peach.rocket.quickstart.event.OrderCreatedEvent;
import com.peach.rocket.quickstart.event.OrderPaidEvent;
import com.peach.rocket.quickstart.example.ConsumeIdempotentExample;
import com.peach.rocket.quickstart.example.OrderOrderlyPublishExample;
import com.peach.rocket.quickstart.example.OrderPublishConsumeExample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证订单发布-消费闭环、顺序 shardingKey 与内存消费幂等。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@SpringBootTest(properties = "quickstart.rocket.demo.enabled=false")
class RocketCapabilityTest {

    @Autowired
    private OrderPublishConsumeExample publishConsumeExample;

    @Autowired
    private OrderOrderlyPublishExample orderlyPublishExample;

    @Autowired
    private ConsumeIdempotentExample idempotentExample;

    @Test
    void shouldDeclareEventAndConsumerRoutes() {
        MqEvent created = OrderCreatedEvent.class.getAnnotation(MqEvent.class);
        MqEvent paid = OrderPaidEvent.class.getAnnotation(MqEvent.class);
        MqConsumer createdConsumer = OrderCreatedConsumer.class.getAnnotation(MqConsumer.class);
        MqConsumer paidConsumer = OrderPaidConsumer.class.getAnnotation(MqConsumer.class);

        assertThat(created).isNotNull();
        assertThat(created.topic()).isEqualTo("order");
        assertThat(created.tag()).isEqualTo("created");
        assertThat(paid).isNotNull();
        assertThat(paid.topic()).isEqualTo("order");
        assertThat(paid.tag()).isEqualTo("paid");
        assertThat(createdConsumer).isNotNull();
        assertThat(createdConsumer.topic()).isEqualTo("order");
        assertThat(createdConsumer.tag()).isEqualTo("created");
        assertThat(paidConsumer).isNotNull();
        assertThat(paidConsumer.topic()).isEqualTo("order");
        assertThat(paidConsumer.tag()).isEqualTo("paid");
    }

    @Test
    void shouldPublishCreatedAndConsumeOnce() {
        MqSendResult result = publishConsumeExample.publishCreated(20001L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.topic()).isEqualTo("order");
        assertThat(result.tag()).isEqualTo("created");
        assertThat(publishConsumeExample.consumedCreated()).hasSize(1);
        assertThat(publishConsumeExample.consumedCreated().get(0).orderId()).isEqualTo(20001L);
    }

    @Test
    void shouldPublishPaidOrderlyWithStableShardingKey() {
        MqSendResult result = orderlyPublishExample.publishPaidOrderly(20002L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(orderlyPublishExample.orderlyShardingKeys()).containsExactly("20002");
        assertThat(orderlyPublishExample.consumedPaid()).hasSize(1);
        assertThat(orderlyPublishExample.consumedPaid().get(0).orderId()).isEqualTo(20002L);
    }

    @Test
    void shouldRejectDuplicateConsumeAfterSuccess() {
        int processed = idempotentExample.consumeTwice("qs:rocket:test:idem:" + UUID.randomUUID());
        assertThat(processed).isEqualTo(1);
    }
}
