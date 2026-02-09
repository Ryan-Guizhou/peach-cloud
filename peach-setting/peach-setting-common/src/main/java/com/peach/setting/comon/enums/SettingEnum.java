package com.peach.setting.comon.enums;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 16:58
 */
public interface SettingEnum {


    /**
     * 业务类型 比如:用户状态、性别、响应等
     */
    enum BizType implements SettingEnum{
        VALUE_SET("VALUE_SET","值集"),
        RESPONSE("RESPONSE","响应状态信息"),
        COMMON("COMMON","后台通用信息"),
        FRONT("FRONT","前端使用")
        ;

        private final String code;

        private final String value;

        BizType (String code,String value){
            this.code = code;
            this.value = value;
        }

        public String getCode(){
            return this.code;
        }

        public String getValue(){
            return this.value;
        }
    }

    /**
     * 值集枚举
     */
    enum ValueSet implements SettingEnum{

        USER_STATUS("USER_STATUS","用户状态"),
        GENDER("GENDER","性别"),
        ;

        private final String code;

        private final String value;

        ValueSet (String code,String value){
            this.code = code;
            this.value = value;
        }

        public String getCode(){
            return this.code;
        }

        public String getValue(){
            return this.value;
        }

    }


}
