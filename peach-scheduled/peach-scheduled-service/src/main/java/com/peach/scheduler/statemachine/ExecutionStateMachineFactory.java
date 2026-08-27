package com.peach.scheduler.statemachine;

import org.springframework.stereotype.Indexed;

import com.peach.scheduled.common.ExecutionEvent;
import com.peach.scheduled.common.ExecutionState;
import java.util.EnumSet;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.support.DefaultStateMachineContext;

/**
 * 创建相关对象。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class ExecutionStateMachineFactory {

    /**
     * 创建相关对象。
     */
    public ExecutionStateMachineFactory() {
        // Intentionally empty.
    }

    /**
     * 构建相关数据。
     *
     * @param current 参数说明
     * @return 返回结果
     */
    @SuppressWarnings("deprecation")
    public StateMachine<ExecutionState, ExecutionEvent> create(ExecutionState current) {
        try {
            StateMachineBuilder.Builder<ExecutionState, ExecutionEvent> builder = StateMachineBuilder.builder();
            builder.configureStates()
                    .withStates()
                    .initial(ExecutionState.CREATED)
                    .states(EnumSet.allOf(ExecutionState.class));
            builder.configureTransitions()
                    .withExternal().source(ExecutionState.CREATED).target(ExecutionState.QUEUED)
                    .event(ExecutionEvent.QUEUE).and()
                    .withExternal().source(ExecutionState.QUEUED).target(ExecutionState.RUNNING)
                    .event(ExecutionEvent.CLAIM).and()
                    .withExternal().source(ExecutionState.RUNNING).target(ExecutionState.SUCCEEDED)
                    .event(ExecutionEvent.SUCCESS).and()
                    .withExternal().source(ExecutionState.RUNNING).target(ExecutionState.RETRY_WAIT)
                    .event(ExecutionEvent.FAIL).and()
                    .withExternal().source(ExecutionState.RUNNING).target(ExecutionState.TIMED_OUT)
                    .event(ExecutionEvent.TIMEOUT).and()
                    .withExternal().source(ExecutionState.RETRY_WAIT).target(ExecutionState.QUEUED)
                    .event(ExecutionEvent.RETRY).and()
                    .withExternal().source(ExecutionState.RETRY_WAIT).target(ExecutionState.DEAD)
                    .event(ExecutionEvent.EXHAUST).and()
                    .withExternal().source(ExecutionState.CREATED).target(ExecutionState.CANCELLED)
                    .event(ExecutionEvent.CANCEL).and()
                    .withExternal().source(ExecutionState.QUEUED).target(ExecutionState.CANCELLED)
                    .event(ExecutionEvent.CANCEL).and()
                    .withExternal().source(ExecutionState.RETRY_WAIT).target(ExecutionState.CANCELLED)
                    .event(ExecutionEvent.CANCEL).and()
                    .withExternal().source(ExecutionState.CREATED).target(ExecutionState.SKIPPED)
                    .event(ExecutionEvent.SKIP);
            StateMachine<ExecutionState, ExecutionEvent> machine = builder.build();
            machine.start();
            machine.getStateMachineAccessor().doWithAllRegions(access -> access.resetStateMachine(
                    new DefaultStateMachineContext<ExecutionState, ExecutionEvent>(current, null, null, null)));
            return machine;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create execution state machine", ex);
        }
    }
}
