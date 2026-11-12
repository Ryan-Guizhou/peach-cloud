package com.peach.rocket.producer;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqDelay;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.route.MqRoute;
import com.peach.rocket.route.MqRouteResolver;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 发送参数和延迟级别的边界验证。 */
class RocketMqPublisherBoundaryTest {

    @Test
    void shouldRejectOutOfRangeDelayBeforeSendingToBroker() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        MqRouteResolver routes = mock(MqRouteResolver.class);
        when(routes.resolve(any(), any())).thenReturn(new MqRoute("orders", "created", "order-1"));
        RocketMqPublisher publisher = new RocketMqPublisher(
                template, mock(MqMessageCodec.class), routes,
                new DefaultMqHeaderResolver(), new PeachRocketProperties());

        assertThatThrownBy(() -> publisher.publishDelay("payload", MqDelay.duration(Duration.ofHours(3))))
                .isInstanceOf(MqException.class).hasMessageContaining("maximum");
        assertThatThrownBy(() -> publisher.publishDelay("payload", MqDelay.duration(Duration.ZERO)))
                .isInstanceOf(MqException.class);
        assertThatThrownBy(() -> publisher.publishDelay("payload", MqDelay.level(19)))
                .isInstanceOf(MqException.class);
        verifyNoInteractions(template);
    }

    @Test
    void shouldNotMutateSourceOptionsOrHeaderMap() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        MqMessageCodec codec = mock(MqMessageCodec.class);
        MqRouteResolver routes = mock(MqRouteResolver.class);
        when(routes.resolve(any(), any())).thenReturn(new MqRoute("orders", "created", "order-1"));
        when(codec.encode(any())).thenReturn(new byte[]{1});
        RocketMqPublisher publisher = new RocketMqPublisher(
                template, codec, routes, new DefaultMqHeaderResolver(), new PeachRocketProperties());
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("tenant", "alpha");
        MqSendOptions original = MqSendOptions.builder()
                .topic("orders").timeoutMillis(0).build();
        original.setHeaders(headers);

        headers.put("tenant", "changed");
        assertThat(original.getHeaders()).containsEntry("tenant", "alpha");
        publisher.publish("payload", original);
        assertThat(original.getTimeoutMillis()).isZero();
        assertThat(original.getHeaders()).containsEntry("tenant", "alpha");

        ArgumentCaptor<MqSendOptions> captor = ArgumentCaptor.forClass(MqSendOptions.class);
        verify(routes).resolve(eq("payload"), captor.capture());
        assertThat(captor.getValue()).isNotSameAs(original);
        assertThat(captor.getValue().getTimeoutMillis()).isEqualTo(3000L);
    }
}
