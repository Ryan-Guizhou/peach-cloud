package com.peach.setting.comon.enums;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description setting 模块常量
 */
public final class SettingConst {

    private SettingConst() {
        throw new IllegalStateException("Utility class");
    }

    public static final String MODULE_CODE = "SETTING";

    public static final String CACHE_DICT_TYPE = "setting:dict:type";

    public static final String CACHE_DICT_ITEM = "setting:dict:item";

    public static final String CACHE_VALUE_SET = "setting:value:set";

    public static final String CACHE_VALUE_SET_ITEM = "setting:value:set:item";

    public static final String CACHE_LANGUAGE = "setting:language";

    public static final String CACHE_MESSAGE = "setting:message";

    public static final String CACHE_NOTICE = "setting:notice";

    public static final String GATEWAY_IP_WHITELIST_KEY = "peach:gateway:risk-control:whitelist-ip";

    public static final String NOTICE_READ_PENDING_LIST = "setting:notice:read:pending:list";

    public static final String NOTICE_READ_PENDING_KEY_PREFIX = "setting:notice:read:pending:{0}:{1}";

    public static final long NOTICE_READ_PENDING_EXPIRE_SECONDS = 24 * 60 * 60L;
}
