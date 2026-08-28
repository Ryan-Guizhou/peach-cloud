package com.peach.scheduler.statemachine;

import org.springframework.stereotype.Indexed;

import com.peach.scheduled.common.JobEvent;
import com.peach.scheduled.common.JobState;
import java.util.EnumSet;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.support.DefaultStateMachineContext;

/**
 * 任务StateMachine工厂。
 * <p>调度模块说明。
 * 调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class JobStateMachineFactory {

    /**
     * 构建相关数据。
     *
     * @param current current。
     * @return 执行结果。
     */
    @SuppressWarnings("deprecation")
    public StateMachine<JobState, JobEvent> create(JobState current) {
        try {
            StateMachineBuilder.Builder<JobState, JobEvent> builder = StateMachineBuilder.builder();
            builder.configureStates()
                    .withStates()
                    .initial(JobState.DRAFT)
                    .states(EnumSet.allOf(JobState.class));
            builder.configureTransitions()
                    .withExternal().source(JobState.DRAFT).target(JobState.ENABLED).event(JobEvent.ENABLE).and()
                    .withExternal().source(JobState.DISABLED).target(JobState.ENABLED).event(JobEvent.ENABLE).and()
                    .withExternal().source(JobState.ENABLED).target(JobState.PAUSED).event(JobEvent.PAUSE).and()
                    .withExternal().source(JobState.PAUSED).target(JobState.ENABLED).event(JobEvent.RESUME).and()
                    .withExternal().source(JobState.ENABLED).target(JobState.DISABLED).event(JobEvent.DISABLE).and()
                    .withExternal().source(JobState.PAUSED).target(JobState.DISABLED).event(JobEvent.DISABLE).and()
                    .withExternal().source(JobState.DRAFT).target(JobState.DELETED).event(JobEvent.DELETE).and()
                    .withExternal().source(JobState.DISABLED).target(JobState.DELETED).event(JobEvent.DELETE);
            StateMachine<JobState, JobEvent> machine = builder.build();
            machine.start();
            machine.getStateMachineAccessor().doWithAllRegions(access -> access.resetStateMachine(
                    new DefaultStateMachineContext<JobState, JobEvent>(current, null, null, null)));
            return machine;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create job state machine", ex);
        }
    }
}
