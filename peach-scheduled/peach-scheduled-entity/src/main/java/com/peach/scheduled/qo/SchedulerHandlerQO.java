package com.peach.scheduled.qo;

import lombok.Data;

/**
 * Handler 管理查询条件，默认包含在线及离线记录。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 22:00
 */
@Data
public class SchedulerHandlerQO {
    /** 应用名称，精确匹配。 */
    private String applicationName;
    /** 心跳状态。 */
    private String status;
    /** 名称或说明关键字。 */
    private String keyword;
}
