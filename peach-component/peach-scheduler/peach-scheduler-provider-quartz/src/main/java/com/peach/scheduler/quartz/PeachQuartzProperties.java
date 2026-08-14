package com.peach.scheduler.quartz;

import org.springframework.stereotype.Indexed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Peach Scheduler Quartz Provider 配置属性。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@ConfigurationProperties(prefix = "peach.scheduler.quartz")
@Indexed
public class PeachQuartzProperties {

    /**
     * Quartz 中 Peach Scheduler 使用的 Job/Trigger group。
     */
    private String group = "PEACH_SCHEDULER";

    /**
     * 创建默认 Quartz Provider 配置。
     */
    public PeachQuartzProperties() {
    }

    /**
     * 获取 Quartz group。
     *
     * @return 返回结果
     */
    public String getGroup() {
        return group;
    }

    /**
     * 设置 Quartz group。
     *
     * @param group 参数说明
     */
    public void setGroup(String group) {
        this.group = group;
    }
}
