package com.peach.scheduler.core;

/**
 * 任务结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public final class JobResult {

    private final boolean success;
    private final String code;
    private final String message;

    private JobResult(boolean success, String code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    /**
     * 创建实例。
     *
     * @return 执行结果。
     */
    public static JobResult success() {
        return new JobResult(true, "SUCCESS", null);
    }

    /**
     * 创建实例。
     *
     * @param code code。
     * @param message message。
     * @return 执行结果。
     */
    public static JobResult failure(String code, String message) {
        return new JobResult(false, code, message);
    }

    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public boolean isSuccess() {
        return success;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public String getCode() {
        return code;
    }
    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    public String getMessage() {
        return message;
    }
}
