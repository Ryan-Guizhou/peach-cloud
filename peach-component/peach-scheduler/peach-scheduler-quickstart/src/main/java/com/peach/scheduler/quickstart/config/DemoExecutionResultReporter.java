package com.peach.scheduler.quickstart.config;

import com.peach.scheduler.transport.ExecutionResultReporter;
import com.peach.scheduler.transport.JobExecutionResultEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 本地结果上报桩：只记录事件，不依赖 RocketMQ Outbox。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
public class DemoExecutionResultReporter implements ExecutionResultReporter {

    private final List<JobExecutionResultEvent> events = Collections.synchronizedList(new ArrayList<>());

    /**
     * 记录执行结果；错误摘要由执行器侧完成脱敏后再传入。
     */
    @Override
    public void report(JobExecutionResultEvent event) {
        events.add(event);
        log.info("demo report, executionId={}, status={}, resultCode={}",
                event.executionId(), event.status(), event.resultCode());
    }

    /**
     * @return 已上报事件的快照
     */
    public List<JobExecutionResultEvent> events() {
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    /**
     * @return 最近一次上报；尚未上报时返回 {@code null}
     */
    public JobExecutionResultEvent lastEvent() {
        synchronized (events) {
            if (events.isEmpty()) {
                return null;
            }
            return events.get(events.size() - 1);
        }
    }

    /**
     * 清空已记录事件。
     */
    public void reset() {
        events.clear();
    }
}
