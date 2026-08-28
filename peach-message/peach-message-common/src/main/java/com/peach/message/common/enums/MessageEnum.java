package com.peach.message.common.enums;

/**
 * 消息模块枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息模块枚举
 */
public interface MessageEnum {

    String getCode();

    String getValue();

    /**
     * Receiver类型枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    enum ReceiverType implements MessageEnum {
        USER("USER", "指定用户"),
        ROLE("ROLE", "指定角色"),
        DEPT("DEPT", "指定部门"),
        TENANT("TENANT", "指定租户"),
        ALL("ALL", "全部在线用户");

        private final String code;

        private final String value;

        ReceiverType(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 消息Category枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    enum MessageCategory implements MessageEnum {
        MESSAGE("MESSAGE", "消息"),
        ANNOUNCEMENT("ANNOUNCEMENT", "公告"),
        TODO("TODO", "待办");

        private final String code;

        private final String value;

        MessageCategory(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 消息类型枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    enum MessageType implements MessageEnum {
        SYSTEM("SYSTEM", "系统消息"),
        NOTICE("NOTICE", "通知消息"),
        APPROVAL("APPROVAL", "审批消息"),
        TASK("TASK", "任务消息"),
        CUSTOM("CUSTOM", "自定义消息");

        private final String code;

        private final String value;

        MessageType(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 消息Source类型枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    enum MessageSourceType implements MessageEnum {
        CUSTOM("CUSTOM", "自定义"),
        MESSAGE("MESSAGE", "消息"),
        ANNOUNCEMENT("ANNOUNCEMENT", "公告"),
        TODO("TODO", "待办");

        private final String code;

        private final String value;

        MessageSourceType(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * SendStatus枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    enum SendStatus implements MessageEnum {
        DRAFT("DRAFT", "草稿"),
        SENT("SENT", "已发送"),
        REVOKED("REVOKED", "已撤销");

        private final String code;

        private final String value;

        SendStatus(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 消息Priority枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    enum MessagePriority implements MessageEnum {
        LOW("LOW", "低"),
        NORMAL("NORMAL", "普通"),
        HIGH("HIGH", "高"),
        URGENT("URGENT", "紧急");

        private final String code;

        private final String value;

        MessagePriority(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * WebSocketEvent类型枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    enum WebSocketEventType implements MessageEnum {
        MESSAGE_CREATED("MESSAGE_CREATED", "新站内信"),
        UNREAD_COUNT_CHANGED("UNREAD_COUNT_CHANGED", "未读数变更"),
        TASK_PROGRESS("TASK_PROGRESS", "任务进度"),
        TASK_FINISHED("TASK_FINISHED", "任务完成"),
        KICK_OUT("KICK_OUT", "踢人下线"),
        PERMISSION_CHANGED("PERMISSION_CHANGED", "权限变更"),
        SYSTEM_NOTICE("SYSTEM_NOTICE", "系统公告");

        private final String code;

        private final String value;

        WebSocketEventType(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * WsPushMode枚举。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    enum WsPushMode implements MessageEnum {
        SINGLE("SINGLE", "单用户推送"),
        MULTI("MULTI", "多用户推送"),
        BROADCAST("BROADCAST", "广播推送");

        private final String code;

        private final String value;

        WsPushMode(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }
}
