package com.peach.threadpool.config;

/**
 * Global配置属性。
 * <p>控制任务提交时需要捕获并在工作线程中恢复的上下文。所有上下文都会在任务完成后清理或
 * 恢复，避免线程复用造成请求信息泄漏。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public class GlobalProperties {

    private boolean enableMdc = true;

    private boolean enableSecurityContext = true;

    public boolean isEnableMdc() {
        return enableMdc;
    }

    public void setEnableMdc(boolean enableMdc) {
        this.enableMdc = enableMdc;
    }

    public boolean isEnableSecurityContext() {
        return enableSecurityContext;
    }

    public void setEnableSecurityContext(boolean enableSecurityContext) {
        this.enableSecurityContext = enableSecurityContext;
    }

}
