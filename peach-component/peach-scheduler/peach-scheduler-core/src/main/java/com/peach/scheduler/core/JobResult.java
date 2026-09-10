package com.peach.scheduler.core;

/**
 * 任务执行结果。
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
     * 创建成功结果。
     *
     * @return 成功任务结果。
     */
    public static JobResult success() {
        return new JobResult(true, "SUCCESS", null);
    }

    /**
     * 创建失败结果。
     *
     * @param code 失败编码。
     * @param message 失败摘要。
     * @return 失败任务结果。
     */
    public static JobResult failure(String code, String message) {
        return new JobResult(false, code, message);
    }

    /**
     * 判断任务是否执行成功。
     *
     * @return 成功返回 {@code true}。
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取结果编码。
     *
     * @return 结果编码。
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取结果摘要。
     *
     * @return 结果摘要。
     */
    public String getMessage() {
        return message;
    }
}
