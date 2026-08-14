package com.peach.scheduler.statemachine;

import org.springframework.statemachine.StateMachine;

/**
 * 校验相关数据。
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
     * 调度模块相关说明。
     *
     * @param machine 参数说明
     * @param event 参数说明
     * 调度模块相关说明。
     * 调度模块相关说明。
     * @return 返回结果
     * @throws IllegalStateException 异常说明
     */
    public static <S, E> S transit(StateMachine<S, E> machine, E event) {
        if (!machine.sendEvent(event)) {
            throw new IllegalStateException("State transition rejected: " + event);
        }
        return machine.getState().getId();
    }
}
