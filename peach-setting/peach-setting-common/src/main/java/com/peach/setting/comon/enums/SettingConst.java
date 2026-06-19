package com.peach.setting.comon.enums;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description setting 模块常量
 */
public interface SettingConst {

    String MODULE_CODE = "SETTING";

    String CACHE_DICT_TYPE = "setting:dict:type";

    String CACHE_DICT_ITEM = "setting:dict:item";

    String CACHE_VALUE_SET = "setting:value:set";

    String CACHE_VALUE_SET_ITEM = "setting:value:set:item";

    String CACHE_LANGUAGE = "setting:language";

    String CACHE_MESSAGE = "setting:message";

    String CACHE_NOTICE = "setting:notice";

    String NOTICE_READ_PENDING_LIST = "setting:notice:read:pending:list";

    String NOTICE_READ_PENDING_KEY_PREFIX = "setting:notice:read:pending:{0}:{1}";

    long NOTICE_READ_PENDING_EXPIRE_SECONDS = 24 * 60 * 60L;
}
