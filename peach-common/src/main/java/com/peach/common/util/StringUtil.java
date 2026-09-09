package com.peach.common.util;

import com.peach.common.constant.PubCommonConst;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类；包含字符串处理、下划线驼峰转换、基础类型安全转换等功能。
 * <p>说明：Date / Timestamp 格式化方法保留以兼容存量调用方。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/14 15:51
 */
@Slf4j
public final class StringUtil implements Serializable {

    @Serial
    private static final long serialVersionUID = 5111636355235107159L;

    // ==========================================
    // 常量定义：正则表达式与常用字面量
    // ==========================================

    private static final Pattern LINE_PATTERN = Pattern.compile("_(\\w)");
    private static final Pattern HUMP_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[\t\r\n]");
    private static final Pattern BLANK_PATTERN = Pattern.compile("\\s*");

    public static final String EMPTY = PubCommonConst.EMPTY;
    private static final String DEFAULT_SPLIT_SPACE = " ";
    private static final String ELLIPSIS = " ...";

    private static final String TRUE_VAL_Y = "y";
    private static final String TRUE_VAL_TRUE = "true";
    private static final String TRUE_VAL_ONE = "1";

    private static final String NUMBER_FORMAT_PATTERN = "#,##0.00";
    private static final String DATE_PATTERN_YYYY_MM_DD = "yyyy-MM-dd";
    private static final String DATETIME_PATTERN_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN_YYYY_MM_DD);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN_YYYY_MM_DD_HH_MM_SS);

    private StringUtil() {
        throw new IllegalStateException("Utility class");
    }

    // ==========================================
    // 基础判空与转换方法
    // ==========================================

    /**
     * 将对象转为字符串，若为 null、"null"（忽略大小写）或空串，则返回空字符串 {@code ""}。
     *
     * @param value 待转换的对象
     * @return 转换后的非 null 字符串
     */
    public static String nullToEmpty(Object value) {
        if (null == value) {
            return EMPTY;
        }
        String tempString = String.valueOf(value);
        if (PubCommonConst.STR_NULL_TEXT.equalsIgnoreCase(tempString) || EMPTY.equals(tempString)) {
            return EMPTY;
        }
        return tempString;
    }

    /**
     * 空值替换（NVL）。若对象为 null、"null" 或空串，则返回指定的默认值。
     *
     * @param value 待检测的对象
     * @param def   默认值
     * @return 转换后的字符串或默认值
     */
    public static String nvl(Object value, String def) {
        if (null == value) {
            return def;
        }
        String tempString = String.valueOf(value);
        if (PubCommonConst.STR_NULL_TEXT.equalsIgnoreCase(tempString) || EMPTY.equals(tempString)) {
            return def;
        }
        return tempString;
    }

    /**
     * 字符串去除首尾空格，若为 null 则返回 null。
     *
     * @param s 待处理字符串
     * @return 去除首尾空格后的字符串或 null
     */
    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * 字符串去除首尾空格并做空值替换，若为 null 则返回默认值。
     *
     * @param s      待处理字符串
     * @param defult 默认值
     * @return 去除首尾空格后的字符串或默认值
     */
    public static String nvl(String s, String defult) {
        return (s == null) ? defult : s.trim();
    }

    /**
     * 字符串去除首尾空格，若为 null 则返回空字符串 {@code ""}。
     *
     * @param s 待处理字符串
     * @return 去除首尾空格后的字符串或空字符串
     */
    public static String nvl(String s) {
        return (s == null) ? EMPTY : s.trim();
    }

    /**
     * 通过反射调用对象单参 String 构造方法实例化泛型对象。
     *
     * @param <T>   目标泛型类型
     * @param value 构造入参值
     * @param clazz 目标对象实例（用于获取 Class）
     * @return 实例化后的泛型对象，若 value 为空则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getGenericsValue(Object value, T clazz) {
        if (isEmpty(value)) {
            return null;
        }
        Object newInstance;
        Constructor<?> constructor;
        try {
            constructor = clazz.getClass().getConstructor(String.class);
            newInstance = constructor.newInstance(value.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T) newInstance;
    }

    /**
     * 判断字符串代表的布尔逻辑是否为“真”（支持 "y", "true", "1"，不区分大小写）。
     *
     * @param val 待判断的字符串
     * @return true 代表逻辑真，否则返回 false
     */
    public static boolean isTrue(String val) {
        if (isEmpty(val)) {
            return false;
        }
        return TRUE_VAL_Y.equalsIgnoreCase(val)
                || TRUE_VAL_TRUE.equalsIgnoreCase(val)
                || TRUE_VAL_ONE.equalsIgnoreCase(val);
    }

    /**
     * 对象转 long 类型，转换失败或为空时默认返回 0。
     *
     * @param value 待转换对象
     * @return long 数值
     */
    public static long toLong(Object value) {
        return toLong(value, 0L);
    }

    /**
     * 对象转 long 类型，转换失败或为空时返回默认值。
     *
     * @param value      待转换对象
     * @param defaultVal 默认值
     * @return long 数值
     */
    public static long toLong(Object value, long defaultVal) {
        try {
            return Long.parseLong(nullToEmpty(value));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /**
     * 生成 32 位无横线 UUID。
     *
     * @return 32 位 UUID
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replace(PubCommonConst.DASH, EMPTY);
    }

    // ==========================================
    // 判空相关方法 (IsEmpty / IsBlank)
    // ==========================================

    /**
     * 判断对象是否为空（null、"null" 文本或空字符串）。
     *
     * @param value 待检测对象
     * @return true 表示为空
     */
    public static boolean isEmpty(Object value) {
        String valueString = nullToEmpty(value);
        return null == valueString
                || PubCommonConst.STR_NULL_TEXT.equalsIgnoreCase(valueString)
                || valueString.isEmpty();
    }

    /**
     * 判断字符串是否不为空。
     *
     * @param value 待检测字符串
     * @return true 表示非空
     */
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    /**
     * 判断对象是否为空白（null、"null" 文本、空字符串或仅包含空白字符）。
     *
     * @param value 待检测对象
     * @return true 表示空白
     */
    public static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        String valueString = String.valueOf(value).trim();
        return valueString.isEmpty() || PubCommonConst.STR_NULL_TEXT.equalsIgnoreCase(valueString);
    }

    /**
     * 判断对象是否非空白。
     *
     * @param value 待检测对象
     * @return true 表示非空白
     */
    public static boolean isNotBlank(Object value) {
        return !isBlank(value);
    }

    /**
     * 可变长参数批量判断：当且仅当所有参数都不为空时返回 true。
     *
     * @param values 待检测字符串数组
     * @return 若包含 null 或空字符串则返回 false，全不为空返回 true
     */
    public static boolean isNotEmpty(String... values) {
        if (values == null || values.length == 0) {
            return Boolean.FALSE;
        }
        for (String value : values) {
            if (null == value || EMPTY.equals(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断 Map 是否非空。
     *
     * @param map 待检测 Map
     * @return true 表示非空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !(map == null || map.isEmpty());
    }

    /**
     * 判断 List 是否非空。
     *
     * @param list 待检测 List
     * @return true 表示非空
     */
    public static boolean isNotEmpty(List<?> list) {
        return !(list == null || list.isEmpty());
    }

    // ==========================================
    // 数字与金额格式化
    // ==========================================

    /**
     * 格式化数字字符串（输出千分位及保留 2 位小数，如：1,234.00）。
     *
     * @param valueString 数字字符串
     * @return 格式化后的数字字符串
     */
    public static String formateNum(String valueString) {
        NumberFormat format = new DecimalFormat(NUMBER_FORMAT_PATTERN);
        BigDecimal bigDecimal = toBigDecimal(valueString);
        return format.format(bigDecimal.doubleValue());
    }

    /**
     * 格式化金额转换为 BigDecimal（保留 2 位小数，四舍五入）。
     *
     * @param valueString 金额字符串
     * @return 格式化后的 BigDecimal 实例（例如：1234.00）
     */
    public static BigDecimal formateNumToDecimal(String valueString) {
        return toBigDecimal(valueString);
    }

    /**
     * 字符串安全转换为 BigDecimal（默认保留 2 位小数，ROUND_HALF_UP）。
     *
     * @param value 字符串数值
     * @return 安全转换后的 BigDecimal，空或无效值返回 0.00
     */
    public static BigDecimal toBigDecimal(String value) {
        BigDecimal decimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (null == value) {
            return decimal;
        }
        if (PubCommonConst.STR_NULL_TEXT.equalsIgnoreCase(value) || EMPTY.equals(value.trim())) {
            return decimal;
        }
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    // ==========================================
    // 日期与时间格式化
    // ==========================================

    /**
     * Timestamp 转日期字符串 (格式: yyyy-MM-dd)。
     *
     * @param timestamp 数据库时间戳
     * @return 格式化日期字符串
     */
    public static String dateToString(Timestamp timestamp) {
        if (timestamp == null) {
            return EMPTY;
        }
        return timestamp.toLocalDateTime().toLocalDate().format(DATE_FORMATTER);
    }

    /**
     * LocalDate 转日期字符串 (格式: yyyy-MM-dd)。
     *
     * @param date 本地日期
     * @return 格式化日期字符串
     */
    public static String dateToString(LocalDate date) {
        if (date == null) {
            return EMPTY;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * LocalDateTime 转完整日期时间字符串 (格式: yyyy-MM-dd HH:mm:ss)。
     *
     * @param date 本地日期时间
     * @return 格式化日期时间字符串
     */
    public static String dateToStrLong(LocalDateTime date) {
        if (date == null) {
            return EMPTY;
        }
        return date.format(DATETIME_FORMATTER);
    }

    // ==========================================
    // 字符串处理与正则表达式操作
    // ==========================================

    /**
     * 清理字符串中的非法字符（制表符 \t、换行符 \r\n）。
     *
     * @param str 待清理字符串
     * @return 清理后的字符串
     */
    public static String celanIllegalChar(String str) {
        if (str != null) {
            Matcher m = ILLEGAL_CHAR_PATTERN.matcher(str);
            return m.replaceAll(EMPTY);
        }
        return str;
    }

    /**
     * 规范名称拼写修正（等价于 {@link #celanIllegalChar(String)}）。
     *
     * @param str 待清理字符串
     * @return 清理后的字符串
     */
    public static String cleanIllegalChar(String str) {
        return celanIllegalChar(str);
    }

    /**
     * 过滤字符串中的所有空白字符（包括空格、制表符、换页符等）。
     *
     * @param str 待处理字符串
     * @return 替换空白后的字符串
     */
    public static String replaceBlank(String str) {
        String dest = EMPTY;
        if (str != null) {
            Matcher m = BLANK_PATTERN.matcher(str);
            dest = m.replaceAll(EMPTY);
        }
        return dest;
    }

    /**
     * 字符串按字符切分为单字符数组。
     *
     * @param string 待切分字符串
     * @return 单字符构成的字符串数组
     */
    public static String[] stringToArray(String string) {
        if (isEmpty(string)) {
            return new String[0];
        }
        String[] result = new String[string.length()];
        for (int i = 0; i < string.length(); i++) {
            result[i] = string.substring(i, i + 1);
        }
        return result;
    }

    /**
     * 下划线分隔大写转换大驼峰样式 (例如: AREM_HOUSE_COLLECT -> AremHouseCollect)。
     *
     * @param originStr 原始下划线字符串
     * @return 大驼峰样式字符串
     */
    public static String getUpperHeadStrNoUnderscore(String originStr) {
        String[] chars = stringToArray(originStr);
        StringBuilder target = new StringBuilder();
        for (int j = 0; j < chars.length; j++) {
            if (j == 0) {
                target.append(chars[j].toUpperCase());
            } else {
                if (chars[j - 1].equals(PubCommonConst.UNDER_LINE)) {
                    target.append(chars[j].toUpperCase());
                } else if (!chars[j].equals(PubCommonConst.UNDER_LINE)) {
                    target.append(chars[j].toLowerCase());
                }
            }
        }
        return target.toString();
    }

    /**
     * 下划线分隔大写转换小驼峰样式 (例如: AREM_HOUSE_COLLECT -> aremHouseCollect)。
     *
     * @param originStr 原始下划线字符串
     * @return 小驼峰样式字符串
     */
    public static String getLowerHeadStrNoUnderscore(String originStr) {
        String[] chars = stringToArray(originStr);
        StringBuilder target = new StringBuilder();
        for (int j = 0; j < chars.length; j++) {
            if (j == 0) {
                target.append(chars[j].toLowerCase());
            } else {
                if (chars[j - 1].equals(PubCommonConst.UNDER_LINE)) {
                    target.append(chars[j].toUpperCase());
                } else if (!chars[j].equals(PubCommonConst.UNDER_LINE)) {
                    target.append(chars[j].toLowerCase());
                }
            }
        }
        return target.toString();
    }

    /**
     * 下划线命名转驼峰命名（全字符串先转小写，下划线后首字母大写）。
     *
     * @param str 包含下划线的字符串
     * @return 驼峰命名字符串
     */
    public static String strTransHump(String str) {
        String[] split = str.split(PubCommonConst.UNDER_LINE);
        StringBuilder resBuilder = new StringBuilder(split[0].toLowerCase());
        for (int i = 1; i < split.length; i++) {
            resBuilder.append(split[i].substring(0, 1).toUpperCase()).append(split[i].substring(1).toLowerCase());
        }
        return resBuilder.toString();
    }

    /**
     * 下划线转驼峰（转为小写后将下划线后的字母转大写）。
     *
     * @param str 待转换字符串
     * @return 驼峰格式字符串
     */
    public static String lineToHump(String str) {
        StringBuilder sb = new StringBuilder();
        if (str != null && !str.isEmpty()) {
            str = str.toLowerCase();
            if (str.contains(PubCommonConst.UNDER_LINE)) {
                Matcher matcher = LINE_PATTERN.matcher(str);
                while (matcher.find()) {
                    matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
                }
                matcher.appendTail(sb);
            } else {
                sb.append(str);
            }
        }
        return sb.toString();
    }

    /**
     * 仅将下划线移除并将其后第一个字母大写，保持其他字母原始大小写不变。
     *
     * @param str 待转换字符串
     * @return 转换后的字符串
     */
    public static String lineToHumpOthersNoChange(String str) {
        if (str == null) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        if (str.contains(PubCommonConst.UNDER_LINE)) {
            Matcher matcher = LINE_PATTERN.matcher(str);
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
            }
            matcher.appendTail(sb);
        } else {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 驼峰转下划线命名。
     *
     * @param str 驼峰命名字符串
     * @return 下划线分隔字符串
     */
    public static String humpToLine(String str) {
        if (str == null) {
            return EMPTY;
        }
        Matcher matcher = HUMP_PATTERN.matcher(str);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, PubCommonConst.UNDER_LINE + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 数字转中文数字文本，保留历史实现语义。
     *
     * @param number 数字
     * @return 中文数字文本
     */
    public static String toChinese(Integer number) {
        String str = Integer.toString(number);
        String[] s1 = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        String[] s2 = {"十", "百", "千", "万", "十", "百", "千", "亿", "十", "百", "千"};
        StringBuilder resultBuilder = new StringBuilder();
        int n = str.length();
        for (int i = 0; i < n; i++) {
            int num = str.charAt(i) - '0';
            if (i != n - 1 && num != 0) {
                resultBuilder.append(s1[num]).append(s2[n - 2 - i]);
            } else {
                resultBuilder.append(s1[num]);
            }
        }
        return resultBuilder.toString();
    }

    // ==========================================
    // 拼接与 Code-Name 组装
    // ==========================================

    /**
     * 组装 Code 和 Name，默认以单空格分隔。
     *
     * @param code 编码
     * @param name 名称
     * @return 组装后的字符串 (例如: "001 名称")
     */
    public static String buildCodeName(String code, String name) {
        return buildCodeName(code, name, DEFAULT_SPLIT_SPACE);
    }

    /**
     * 组装 Code 和 Name，使用指定分隔符。
     *
     * @param code  编码
     * @param name  名称
     * @param split 自定义分隔符
     * @return 组装后的字符串
     */
    public static String buildCodeName(String code, String name, String split) {
        StringBuilder content = new StringBuilder();
        if (isNotEmpty(code)) {
            if (isEmpty(split)) {
                split = DEFAULT_SPLIT_SPACE;
            }
            content.append(code).append(split);
        }

        if (isNotEmpty(name)) {
            content.append(name);
        }
        return content.toString();
    }

    // ==========================================
    // URL 编解码
    // ==========================================

    /**
     * 使用 UTF-8 字符集进行 URL 解码。
     *
     * @param str 待解码字符串
     * @return 解码后的字符串
     */
    public static String urlDecode(String str) {
        try {
            str = URLDecoder.decode(str, PubCommonConst.UTF_8);
        } catch (Exception e) {
            log.debug("URL decode failed", e);
        }
        return str;
    }

    /**
     * 使用 UTF-8 字符集进行 URL 编码。
     *
     * @param str 待编码字符串
     * @return 编码后的字符串
     */
    public static String urlEncode(String str) {
        try {
            str = URLEncoder.encode(str, PubCommonConst.UTF_8);
        } catch (UnsupportedEncodingException e) {
            log.debug("URL encode failed", e);
        }
        return str;
    }

    /**
     * 对文件名进行 URL 编码，仅对文件名主体进行编码，保留扩展名后缀点号及后缀文本。
     *
     * @param fileName 完整文件名（如: "测试文件.pdf"）
     * @return 编码后的文件名
     */
    public static String urlEncodeFielName(String fileName) {
        String fileNamePrefix;
        String fileNameSuffix;
        if (fileName.lastIndexOf(PubCommonConst.DOT) > 0) {
            fileNamePrefix = fileName.substring(0, fileName.lastIndexOf(PubCommonConst.DOT));
            fileNameSuffix = fileName.replaceAll(fileNamePrefix, EMPTY);
        } else {
            fileNamePrefix = fileName;
            fileNameSuffix = EMPTY;
        }
        return urlEncode(fileNamePrefix) + fileNameSuffix;
    }

    // ==========================================
    // 集合拼接与字符串格式化
    // ==========================================

    /**
     * 格式化字符串集合，默认使用逗号分隔，并支持限制最大拼接元素数。
     *
     * @param strs  字符串集合
     * @param limit 限制显示数量（超出则追加 " ..."，0 表示不限制）
     * @return 格式化后的字符串
     */
    public static String formatStrList(List<String> strs, Integer limit) {
        return formatStrList(strs, PubCommonConst.COMMA, limit);
    }

    /**
     * 使用自定义分隔符格式化字符串集合，并支持限制最大拼接元素数。
     *
     * @param strs      字符串集合
     * @param separator 分隔符
     * @param limit     限制显示数量（超出则追加 " ..."，0 表示不限制）
     * @return 格式化后的字符串
     */
    public static String formatStrList(List<String> strs, String separator, Integer limit) {
        if (isEmpty(separator)) {
            separator = PubCommonConst.COMMA;
        }
        StringBuilder res = new StringBuilder();
        if (PeachCollectionUtil.isNotEmpty(strs)) {
            for (int i = 0; i < strs.size(); i++) {
                String s = strs.get(i);
                if (limit != 0 && i == limit) {
                    res.append(ELLIPSIS);
                } else {
                    if (isEmpty(res.toString())) {
                        res.append(s);
                    } else {
                        res.append(separator).append(s);
                    }
                }
            }
        }
        return res.toString();
    }

    /**
     * 安全提取对象字符串表示（仅处理 BigDecimal、Integer、String 类型）。
     *
     * @param value 目标对象
     * @return 字符串值，无效或空时返回空字符串 {@code ""}
     */
    public static String getStringValue(Object value) {
        return getStringValue(value, EMPTY);
    }

    /**
     * 安全提取对象字符串表示（仅处理 BigDecimal、Integer、String 类型）。
     *
     * @param value        目标对象
     * @param defalutValue 默认值
     * @return 字符串值或默认值
     */
    public static String getStringValue(Object value, String defalutValue) {
        String finalValue = defalutValue;
        try {
            if (null != value) {
                if (value instanceof BigDecimal temp) {
                    finalValue = temp.toString();
                } else if (value instanceof Integer temp) {
                    finalValue = temp.toString();
                } else if (value instanceof String str) {
                    finalValue = str;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return finalValue;
    }

    // ==========================================
    // 补充常用扩展工具方法 (新增)
    // ==========================================


    /**
     * 基于 Java StandardCharsets 实现的无受检异常 URL 编码 (推荐现代 JDK 调用)。
     *
     * @param str 待编码字符串
     * @return 编码后的字符串
     */
    public static String encodeUtf8(String str) {
        return str == null ? EMPTY : URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    /**
     * 基于 Java StandardCharsets 实现的无受检异常 URL 解码 (推荐现代 JDK 调用)。
     *
     * @param str 待解码字符串
     * @return 解码后的字符串
     */
    public static String decodeUtf8(String str) {
        return str == null ? EMPTY : URLDecoder.decode(str, StandardCharsets.UTF_8);
    }
}
