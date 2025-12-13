package com.peach.common.util.language;

import cn.hutool.setting.dialect.Props;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/13 12:44
 */
@Slf4j
public final class MultiLanguage {


    private static final String DEFAULT_LANGUAGE = "zh";

    private static final String DEFAULT_COMMON_PATH = "i18n.common";

    private static final String DEFAULT_RESPONE_PATH = "i18n.response";

    private static final String DEFAULT_EXCEPTION_PATH = "i18n.exception";

    public static final String FILE_PATH = "{0}_{1}_{2}.properties";

    private static final Cache<String, Props> CAFFINE_CACHE = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();

    private MultiLanguage() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取多语言
     * @param keyword 关键字
     * @param language 语言
     * @return 多语言
     */
    public static String getResponseMultiLanguage(String keyword,String language) {
        return getMultiLanguage(keyword,language,DEFAULT_RESPONE_PATH);
    }

    /**
     * 获取多语言
     * @param keyword 关键字
     * @return 多语言
     */
    @Deprecated
    public static String getExceptionMultiLanguage(String keyword) {
        return getMultiLanguage(keyword,DEFAULT_LANGUAGE,DEFAULT_EXCEPTION_PATH);
    }

    /**
     * 获取多语言
     * @param keyword 关键字
     * @param language 语言
     * @return 多语言
     */
    public static String getExceptionMultiLanguage(String keyword,String language) {
        return getMultiLanguage(keyword,language,DEFAULT_EXCEPTION_PATH);
    }

    /**
     * 获取多语言
     * @param keyword 关键字
     * @return 多语言
     */
    @Deprecated
    public static String getResponseMultiLanguage(String keyword) {
        return getMultiLanguage(keyword,DEFAULT_LANGUAGE,DEFAULT_RESPONE_PATH);
    }

    /**
     * 获取多语言
     * @param keyword 关键字
     * @param language 语言
     * @return 多语言
     */
    public static String getCommonMultiLanguage(String keyword,String language) {
        return getMultiLanguage(keyword,language,DEFAULT_COMMON_PATH);
    }

    /**
     * 获取多语言
     * @param keyword 关键字
     * @return 多语言
     */
    @Deprecated
    public static String getCommonMultiLanguage(String keyword) {
        return getMultiLanguage(keyword,DEFAULT_LANGUAGE,DEFAULT_COMMON_PATH);
    }


    /**
     * 获取多语言
     * @param keyword 关键字
     * @param language 语言
     * @param useType 使用类型
     * @return 多语言
     */
    public static String getMultiLanguage(String keyword,String language,String useType) {
        //1、计算城市
        String country = Locale.SIMPLIFIED_CHINESE.getCountry();
        if(Locale.US.getLanguage().equals(language)){
            country = Locale.US.getCountry();
        }else if(Locale.KOREA.getLanguage().equals(language)){
            country = Locale.KOREA.getCountry();
        }

        String fileName = MessageFormat.format(FILE_PATH,useType.replace(".", "/"),language,country);
        Props props = CAFFINE_CACHE.get(buildKey(language, useType, country), key -> new Props(fileName, StandardCharsets.UTF_8));
        Object o = props.get(keyword);
        if (o == null){
            return null;
        }
        return StringUtil.getStringValue(o);
    }

    /**
     * 构建key
     * @param language 语言
     * @param useType 使用类型
     * @param country 国家
     * @return key
     */
    private static String buildKey(String language,String useType,String country) {
        return useType.replace("i18n","") + "_" + language + "_" + country;
    }

    public static void main(String[] args) {
        System.out.println(getMultiLanguage("fail", "zh", "i18n.response"));
    }

}
