package com.peach.setting.comon.enums;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description setting 模块通用枚举接口
 */
public interface SettingEnum {

    /**
     * 获取编码。
     *
     * @return 编码
     */
    String getCode();

    /**
     * 获取描述。
     *
     * @return 描述
     */
    String getValue();

    /**
     * 业务分类。
     */
    enum BizType implements SettingEnum {
        VALUE_SET("VALUE_SET", "值集"),
        RESPONSE("RESPONSE", "响应"),
        COMMON("COMMON", "通用"),
        FRONT("FRONT", "前端"),
        DICT("DICT", "字典"),
        NOTICE("NOTICE", "公告"),
        I18N("I18N", "多语言");

        private final String code;
        private final String value;

        BizType(String code, String value) {
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
     * 值集来源类型。
     */
    enum ValueSetSourceType implements SettingEnum {
        DICT("DICT", "字典"),
        CUSTOM("CUSTOM", "自定义");

        private final String code;
        private final String value;

        ValueSetSourceType(String code, String value) {
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
     * 公告类型。
     */
    enum NoticeType implements SettingEnum {
        INFO("INFO", "普通公告"),
        WARNING("WARNING", "警告公告"),
        MAINTENANCE("MAINTENANCE", "维护公告"),
        PROMOTION("PROMOTION", "活动公告");

        private final String code;
        private final String value;

        NoticeType(String code, String value) {
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
     * 发布状态。
     */
    enum PublishStatus implements SettingEnum {
        DRAFT("DRAFT", "草稿"),
        PUBLISHED("PUBLISHED", "已发布"),
        REVOKED("REVOKED", "已撤销"),
        OFFLINE("OFFLINE", "已下线");

        private final String code;
        private final String value;

        PublishStatus(String code, String value) {
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
     * 消息类型。
     */
    enum MessageType implements SettingEnum {
        NOTICE("NOTICE", "通知"),
        ANNOUNCEMENT("ANNOUNCEMENT", "公告"),
        SYSTEM("SYSTEM", "系统消息"),
        TODO("TODO", "待办"),
        TEXT("TEXT", "普通文本"),
        DICT("DICT", "字典文案"),
        VALUE_SET("VALUE_SET", "值集文案"),
        ERROR("ERROR", "错误提示"),
        VALIDATION("VALIDATION", "校验提示"),
        BUTTON("BUTTON", "按钮文案"),
        TITLE("TITLE", "标题文案"),
        FIELD("FIELD", "字段文案"),
        PLACEHOLDER("PLACEHOLDER", "占位文案"),
        FRONTEND("FRONTEND", "前端文案"),
        BACKEND("BACKEND", "后端文案");

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
     * 消息来源类型。
     */
    enum MessageSourceType implements SettingEnum {
        CUSTOM("CUSTOM", "自定义"),
        ANNOUNCEMENT("ANNOUNCEMENT", "公告");

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
     * 发送状态。
     */
    enum SendStatus implements SettingEnum {
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
     * 使用范围。
     */
    enum UsageScope implements SettingEnum {
        COMMON("COMMON", "通用"),
        BACKEND("BACKEND", "后端"),
        FRONTEND("FRONTEND", "前端"),
        BOTH("BOTH", "前后端共用");

        private final String code;
        private final String value;

        UsageScope(String code, String value) {
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
