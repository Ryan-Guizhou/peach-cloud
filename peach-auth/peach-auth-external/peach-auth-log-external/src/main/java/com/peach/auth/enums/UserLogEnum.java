package com.peach.auth.enums;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 17:57
 */
public interface UserLogEnum {

    /**
     * 模块枚举
     */
    enum Module implements UserLogEnum{
        USERSERVICE("USERSERVICE", "用户服务"),

        FILESERVICE("FILESERVICE","文件服务"),

        MONIORSERVICE("MONIORSERVICE","监控服务"),

        SCHEDULESERVICE("SCHEDULESERVICE","定时任务模块"),

        SYSCONFIG("SYSCONFIG","系统配置"),

        SETTING("SETTING","系统设置"),

        DEFAULT("UNKNOWN","未知"),
        GENERATOR("GENERATOR","未知"),
        ;

        private final String moduleCode;

        private final String moduleName;

        Module(String moduleCode, String moduleName) {
            this.moduleCode = moduleCode;
            this.moduleName = moduleName;
        }

        public String getModuleCode() {
            return moduleCode;
        }

        public String getModuleName() {
            return moduleName;
        }

        public static Module getModuleEnumByModuleCode(String moduleCode) {
            for (Module moduleEnum : Module.values()) {
                if (moduleEnum.getModuleCode().equals(moduleCode)) {
                    return moduleEnum;
                }
            }
            return null;
        }
    }

    /**
     * 操作类型枚举
     */
    enum OptType implements UserLogEnum{


        DELETE("DELETE","删除"),

        SELECT("SELECT","查询"),

        INSERT("INSERT","新增"),

        UPDATE("UPDATE","更新"),

        DEFAULT("UNKNOWN","未知"),
        ;

        private final String optTypeCode;

        private final String optTypeName;

        OptType(String optTypeCode, String optTypeName) {
            this.optTypeCode = optTypeCode;
            this.optTypeName = optTypeName;
        }

        public String getOptTypeCode() {
            return optTypeCode;
        }

        public String getOptTypeName() {
            return optTypeName;
        }
        
        public OptType getOptTypeEnumByOptTypeCode(String optTypeCode) {
            for (OptType optTypeEnum : OptType.values()) {
                if (optTypeEnum.getOptTypeCode().equals(optTypeCode)) {
                    return optTypeEnum;
                }
            }
            return null;
        }
    }

    /**
     * 日志级别枚举
     */
    enum LogLevel implements UserLogEnum{

        INFO("INFO","INFO"),

        DEBUG("DEBUG","DEBUG"),

        WARN("WARN","WARN"),

        ERROR("ERROR","ERROR"),

        DEFAULT("UNKNOWN","未知"),
        ;

        private final String logLevelCode;

        private final String logLevelName;

        LogLevel(String logLevelCode, String logLevelName) {
            this.logLevelCode = logLevelCode;
            this.logLevelName = logLevelName;
        }

        public String getLogLevelCode() {
            return logLevelCode;
        }

        public String getLogLevelName() {
            return logLevelName;
        }

        public LogLevel getLogLevelEnumByLogLevelCode(String logLevelCode) {
            for (LogLevel logLevelEnum : LogLevel.values()) {
                if (logLevelEnum.getLogLevelCode().equals(logLevelCode)) {
                    return logLevelEnum;
                }
            }
            return null;
        }
    }
}
