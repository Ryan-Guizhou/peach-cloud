package com.peach.common;

import com.peach.common.util.StringUtil;

import java.util.UUID;

/**
 * // UUID生成器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2024/10/10 15:22
 * @Description // UUID生成器
 */
public class IDGeneratorUtil {

    private IDGeneratorUtil() {
        throw new IllegalStateException("Utility class");
    }
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

    public static String generateUuid() {
        String uuid = UUID.randomUUID().toString();
        String replace = uuid.replace(UUID_SEPARATOR, DEFAULE_SEPARATOR);
        return replace.substring(START_INDEX, MAX_LENGTH);
    }

}
