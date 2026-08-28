package com.peach.scheduler.statemachine;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

/**
 * StateMachineTransition解析器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public final class StateMachineTransitionResolver {

    /**
     * 工具类说明。
     */
    private StateMachineTransitionResolver() {
    }

    /**
     * 调度模块说明。
     *
     * @param machine machine。
     * @param event event。
     * 调度模块说明。
     * 调度模块说明。
     * @return 执行结果。
     * @throws IllegalStateException 异常说明
     */
    public static <S, E> S transit(StateMachine<S, E> machine, E event) {
        StateMachineEventResult<S, E> result = machine.sendEvent(
                Mono.just(MessageBuilder.withPayload(event).build())
        ).blockLast();
        if (result == null || result.getResultType() != StateMachineEventResult.ResultType.ACCEPTED) {
            throw new IllegalStateException("State transition rejected: " + event);
        }
        return machine.getState().getId();
    }
}
