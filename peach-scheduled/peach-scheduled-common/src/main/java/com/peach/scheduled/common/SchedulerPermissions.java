package com.peach.scheduled.common;

/**
 * Scheduler 管理端操作级权限编码。
 *
 * <p>常量用于前端路由、权限资源初始化以及后续接入统一接口鉴权；
 * 平台侧 `scheduler:*` 资源由权限模块维护，编码以本类为准。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public final class SchedulerPermissions {

    /**
     * 查看任务列表。
     */
    public static final String JOB_LIST = "scheduler:job:list";

    /**
     * 查看任务详情。
     */
    public static final String JOB_DETAIL = "scheduler:job:detail";

    /**
     * 创建任务。
     */
    public static final String JOB_CREATE = "scheduler:job:create";

    /**
     * 修改任务定义。
     */
    public static final String JOB_UPDATE = "scheduler:job:update";

    /**
     * 启用任务。
     */
    public static final String JOB_ENABLE = "scheduler:job:enable";

    /**
     * 禁用任务。
     */
    public static final String JOB_DISABLE = "scheduler:job:disable";

    /**
     * 暂停任务。
     */
    public static final String JOB_PAUSE = "scheduler:job:pause";

    /**
     * 恢复任务。
     */
    public static final String JOB_RESUME = "scheduler:job:resume";

    /**
     * 立即执行任务。
     */
    public static final String JOB_RUN = "scheduler:job:run";

    /**
     * 删除任务。
     */
    public static final String JOB_DELETE = "scheduler:job:delete";

    /**
     * 查看执行历史。
     */
    public static final String EXECUTION_LIST = "scheduler:execution:list";

    /**
     * 查看执行详情。
     */
    public static final String EXECUTION_DETAIL = "scheduler:execution:detail";

    /**
     * 手工重试等待中的执行实例。
     */
    public static final String EXECUTION_RETRY = "scheduler:execution:retry";

    /**
     * 取消尚未开始业务执行的执行实例。
     */
    public static final String EXECUTION_CANCEL = "scheduler:execution:cancel";

    /**
     * 查看已注册 Handler。
     */
    public static final String HANDLER_LIST = "scheduler:handler:list";

    /**
     * 工具类不允许实例化。
     */
    private SchedulerPermissions() {
    }
}
