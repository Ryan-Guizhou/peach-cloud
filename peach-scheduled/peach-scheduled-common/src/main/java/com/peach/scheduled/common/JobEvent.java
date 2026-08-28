package com.peach.scheduled.common;

/**
 * 任务事件。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public enum JobEvent {

    /**
     * 启用任务。
     */
    ENABLE,

    /**
     * 暂停任务。
     */
    PAUSE,

    /**
     * 恢复已暂停任务。
     */
    RESUME,

    /**
     * 禁用任务。
     */
    DISABLE,

    /**
     * 软删除任务。
     */
    DELETE
}
