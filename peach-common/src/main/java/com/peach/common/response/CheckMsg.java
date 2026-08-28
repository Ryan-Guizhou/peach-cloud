package com.peach.common.response;

import java.io.Serial;

import java.io.Serializable;

/**
 * 校验消息类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 9:45
 * @Description 校验消息类
 */
public class CheckMsg implements Serializable {

    @Serial
    private static final long serialVersionUID = -6951787632913350881L;

    private String msgInfo;

    private boolean success = true;

    private transient Object data;

    public CheckMsg() {
        // Required for JavaBean serialization.
    }

    public String getMsgInfo() {
        return msgInfo;
    }

    public CheckMsg setMsgInfo(String msgInfo) {
        this.msgInfo = msgInfo;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public CheckMsg setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public Object getData() {
        return data;
    }

    public CheckMsg setData(Object data ) {
        this.data = data;
        return this;
    }

    public CheckMsg success(CheckCallback callback) {
        if (this.isSuccess()) {
            return callback.doCallback();
        }
        return this;
    }

    public CheckMsg fail(CheckCallback callback) {
        if (!this.isSuccess()) {
            callback.doCallback();
        }
        return this;
    }

    public static CheckMsg fail(String... msg) {
        CheckMsg check = new CheckMsg();
        check.setSuccess(false);
        if (msg != null && msg.length > 0) {
            check.setMsgInfo(msg[0]);
        }
        return check;
    }

    public static CheckMsg success(String... msg) {
        CheckMsg check = new CheckMsg();
        check.setSuccess(true);
        if (msg != null && msg.length > 0) {
            check.setMsgInfo(msg[0]);
        }
        return check;
    }

    /**
     * Check回调。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    public static interface CheckCallback {
        CheckMsg doCallback();
    }

}
