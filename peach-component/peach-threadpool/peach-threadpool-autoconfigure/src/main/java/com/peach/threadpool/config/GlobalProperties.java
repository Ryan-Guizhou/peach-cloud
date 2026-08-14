package com.peach.threadpool.config;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 17:51
 */
public class GlobalProperties {

    private boolean enableSecurityContext = true;


    public boolean isEnableSecurityContext() {
        return enableSecurityContext;
    }

    public void setEnableSecurityContext(boolean enableSecurityContext) {
        this.enableSecurityContext = enableSecurityContext;
    }

}
