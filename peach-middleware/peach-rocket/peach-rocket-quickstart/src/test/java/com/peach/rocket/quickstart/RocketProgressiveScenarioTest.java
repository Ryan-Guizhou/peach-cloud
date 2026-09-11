package com.peach.rocket.quickstart;

import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.quickstart.runner.RocketProgressiveDemoRunner;
import com.peach.rocket.quickstart.scenario.ProgressiveDemo;
import com.peach.rocket.quickstart.scenario.advanced.JdbcIdempotentStoreContractDemo;
import com.peach.rocket.quickstart.scenario.advanced.JdbcOutboxStoreContractDemo;
import com.peach.rocket.quickstart.scenario.basic.MqConsumerDeclarationDemo;
import com.peach.rocket.quickstart.scenario.basic.MqEventDeclarationDemo;
import com.peach.rocket.quickstart.scenario.intermediate.OrderCreatedPublishDemo;
import com.peach.rocket.quickstart.scenario.intermediate.OrderPaidOrderlyPublishDemo;
import com.peach.rocket.quickstart.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * 无外部 RocketMQ 依赖的递进演示与契约验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:10
 */
@ExtendWith(MockitoExtension.class)
class RocketProgressiveScenarioTest {

    @Mock
    private MqPublisher mqPublisher;

    @Test
    void shouldExposeProgressiveDemosAcrossThreeLevels() {
        List<ProgressiveDemo> demos = demos(false);
        assertThat(demos).hasSizeGreaterThanOrEqualTo(6);
        long levels = demos.stream().map(ProgressiveDemo::level).distinct().count();
        assertThat(levels).isEqualTo(3);
    }

    @Test
    void shouldExecuteAllProgressiveDemosWithoutExternalMq() {
        List<ProgressiveDemo> ordered = demos(false).stream()
                .sorted(Comparator.comparingInt((ProgressiveDemo demo) -> demo.level().order())
                        .thenComparingInt(ProgressiveDemo::order))
                .toList();
        for (ProgressiveDemo demo : ordered) {
            demo.execute();
        }
    }

    @Test
    void shouldPublishWhenRequireMqEnabled() {
        OrderService orderService = new OrderService(mqPublisher);
        ProgressiveDemo demo = new OrderCreatedPublishDemo(orderService, mqPublisher, true);
        demo.execute();
        verify(mqPublisher, atLeastOnce()).publish(any());
        verify(mqPublisher, atLeastOnce()).publishAsync(any());
    }

    @Test
    void shouldAllowDisablingDemoRunner() {
        ConditionalOnProperty condition = RocketProgressiveDemoRunner.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(condition).isNotNull();
        assertThat(condition.prefix()).isEqualTo("quickstart.rocket.demo");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isTrue();
    }

    private List<ProgressiveDemo> demos(boolean requireMq) {
        OrderService orderService = new OrderService(mqPublisher);
        return List.of(
                new MqEventDeclarationDemo(),
                new MqConsumerDeclarationDemo(),
                new OrderCreatedPublishDemo(orderService, mqPublisher, requireMq),
                new OrderPaidOrderlyPublishDemo(orderService, mqPublisher, requireMq),
                new JdbcIdempotentStoreContractDemo(),
                new JdbcOutboxStoreContractDemo()
        );
    }
}
