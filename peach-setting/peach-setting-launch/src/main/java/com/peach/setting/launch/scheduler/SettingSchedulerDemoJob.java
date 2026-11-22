package com.peach.setting.launch.scheduler;

import com.peach.scheduler.annotation.PeachJob;
import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobHandler;
import com.peach.scheduler.core.JobResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * setting 服务调度联调任务，仅返回成功，不读取或修改业务数据。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 22:00
 */
@Indexed
@Component
@PeachJob(value = "settingSchedulerDemo", jobCode = "setting_scheduler_interval_demo",
        jobName = "Setting 固定周期联调测试", description = "setting 服务调度链路测试")
@ConditionalOnProperty(name = "peach.scheduler.demo.enabled", havingValue = "true")
public class SettingSchedulerDemoJob implements JobHandler {
    /**
     * 验证分发、租约和结果回报链路。
     *
     * @param context 执行上下文
     * @return 成功结果
     */
    @Override
    public JobResult execute(JobContext context) {
        return JobResult.success();
    }
}
