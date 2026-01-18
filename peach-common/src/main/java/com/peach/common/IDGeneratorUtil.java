package com.peach.common;

import com.peach.common.util.StringUtil;

import java.util.UUID;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // UUID生成器
 * @CreateTime 2024/10/10 15:22
 */
public class IDGeneratorUtil {
    /**
     * 开始的索引值
     */
    public static final Integer START_INDEX = 0;

    /**
     * UUID 分隔符
     */
    public static final String UUID_SEPARATOR = "-";

    /**
     * 默认分隔符
     */
    public static final String DEFAULE_SEPARATOR = StringUtil.EMPTY;

    /**
     * 最大长度为32位
     */
    public static final Integer MAX_LENGTH = 32;

    public static String UUID() {
        String uuid = UUID.randomUUID().toString();
        String replace = uuid.replace(UUID_SEPARATOR, DEFAULE_SEPARATOR);
        return replace.substring(START_INDEX, MAX_LENGTH);
    }

}
