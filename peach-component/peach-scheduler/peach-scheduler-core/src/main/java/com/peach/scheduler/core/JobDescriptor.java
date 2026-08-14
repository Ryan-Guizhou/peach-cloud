package com.peach.scheduler.core;

/**
 * 调度模块相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class JobDescriptor {
    private String handlerName;
    private String description;

    /**
     * 创建相关对象。
     */
    public JobDescriptor() {
    }

    /**
     * 创建相关对象。
     * @param handlerName 参数说明
     * @param description 参数说明
     */
    public JobDescriptor(String handlerName, String description) {
        this.handlerName = handlerName;
        this.description = description;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getHandlerName() {
        return handlerName;
    }
    /**
     * 设置相关数据。
     *
     * @param handlerName 参数说明
     */
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getDescription() {
        return description;
    }
    /**
     * 设置相关数据。
     *
     * @param description 参数说明
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
