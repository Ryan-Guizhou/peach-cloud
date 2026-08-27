package com.peach.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import lombok.extern.slf4j.Slf4j;

import com.peach.common.constant.PubCommonConst;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 字符串工具；Date 格式化方法保留以兼容存量调用方。
 * @CreateTime 2025/10/14 15:51
  */
@Slf4j
public final class StringUtil implements Serializable {

    private static final long serialVersionUID = 2143367839995921470L;

    private static final Pattern linePattern = Pattern.compile("_(\\w)");

    private static final Pattern humpPattern = Pattern.compile("[A-Z]");

    public static final String EMPTY = "";

    public static final String SEPARATOR_COLON = ":";

    public static final String SEPARATOR_VERTICAL_LINE = "|";

    public static final String UNDER_LINE = "_";

    public static final String COMMA = ",";

    public static final Integer UN_LIMIT = 0;

    private StringUtil() {
        throw new IllegalStateException("Utility class");
    }


    public static String nullToEmpty(Object value) {
        if (null == value) {
            return "";
        }
        String tempString = String.valueOf(value);
        if ("null".equalsIgnoreCase(tempString) || "".equals(tempString)) {
            return "";
        }
        return tempString;
    }

    public static String nvl(Object value, String def) {
        if (null == value) {
            return def;
        }
        String tempString = String.valueOf(value);
        if ("null".equalsIgnoreCase(tempString) || "".equals(tempString)) {
            return def;
        }
        return tempString;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getGenericsValue(Object value, T clazz) {
        if (isEmpty(value)) {
            return null;
        }
        Object newInstance = null;
        Constructor<? extends Object> constructor;
        try {
            constructor = clazz.getClass().getConstructor(String.class);
            if (constructor != null) {
                newInstance = constructor.newInstance(value.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T) newInstance;
    }

    /**
     * @param val
     * @return
     * @describe 判断一个字符是否是“是”
     */
    public static boolean isTrue(String val) {
        if (isEmpty(val)) {
            return false;
        }
        return "y".equalsIgnoreCase(val) || "true".equalsIgnoreCase(val) || "1".equalsIgnoreCase(val);
    }

    /**
     * @param value
     * @return
     * @describe 字符串转换成longl类型，默认为0
     */
    public static long toLong(Object value) {
        return toLong(value, 0);
    }

    /**
     * @param value      要转换的值
     * @param defaultVal 默认的值，报错的时候返回默认值
     * @return
     * @describe 字符串转换成long
     */
    public static long toLong(Object value, long defaultVal) {
        try {
            return Long.parseLong(nullToEmpty(value));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static boolean isEmpty(Object value) {
        String valueString = nullToEmpty(value);
        return null == valueString
                || "null".equalsIgnoreCase(valueString)
                || valueString.isEmpty();
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }


    public static boolean isBlank(Object value){
        return isEmpty(value);
    }

    public static boolean isNotBlank(Object value){
        return !isBlank(value);
    }

    /**
     * 判断是否为空
     *
     * @param values
     * @return
     */
    public static boolean isNotEmpty(String... values) {
        boolean res = true;
        if (values == null || values.length == 0) {
            return Boolean.FALSE;
        }
        for (String value : values) {
            if (null == value || "".equals(value)) {
                res = false;
                break;
            }
        }
        return res;
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !(map == null || map.isEmpty());
    }

    public static boolean isNotEmpty(List<?> list) {
        return !(list == null || list.isEmpty());
    }

    /**
     * @return
     * @describe 获得一个随机的编码
     */
    public static String getUUID() {
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.replace("-", "");
        return uuid;
    }

    public static String formateNum(String valueString) {
        NumberFormat format = new DecimalFormat("#,##0.00");
        BigDecimal bigDecimal = toBigDecimal(valueString);
        return format.format(bigDecimal.doubleValue());
    }

    /**
     * 格式化金额
     *
     * @param valueString
     * @return 格式化后的金额（不包含千分位逗号显示，例如：1234.00）
     */
    public static BigDecimal formateNumToDecimal(String valueString) {
        return toBigDecimal(valueString);
    }

    public static BigDecimal toBigDecimal(String value) {
        BigDecimal decimal = BigDecimal.ZERO;
        decimal = decimal.setScale(2, RoundingMode.HALF_UP);
        if (null == value) {
            return decimal;
        }
        if ("null".equalsIgnoreCase(value) || "".equals(value.trim())) {
            return decimal;
        }
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * @param timestamp
     * @return
     * @describe 日期转换成字符串
     */
    public static String dateToString(Timestamp timestamp) {
        if (timestamp == null) {
            return EMPTY;
        }
        return timestamp.toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * @describe string字符串转数组
     * @parms
     */
    public static String[] stringToArray(String string) {
        if (isEmpty(string))
            return new String[0];
        String[] result = new String[string.length()];
        for (int i = 0; i < string.length(); i++) {
            result[i] = string.substring(i, i + 1);
        }
        return result;
    }

    /**
     * @param originStr
     * @return
     * @describe AREM_HOUSE_COLLECT --> AremHouseCollect
     */
    public static String getUpperHeadStrNoUnderscore(String originStr) {
        String[] chars = StringUtil.stringToArray(originStr);
        StringBuffer target = new StringBuffer();
        for (int j = 0; j < chars.length; j++) {
            if (j == 0) {
                target.append(chars[j].toUpperCase());
            } else {
                if (chars[j - 1].equals("_")) {
                    target.append(chars[j].toUpperCase());
                } else if (!chars[j].equals("_"))
                    target.append(chars[j].toLowerCase());
            }
        }
        return target.toString();
    }

    /**
     * @param originStr
     * @return
     * @describe AREM_HOUSE_COLLECT --> aremHouseCollect
     */
    public static String getLowerHeadStrNoUnderscore(String originStr) {
        String[] chars = StringUtil.stringToArray(originStr);
        StringBuffer target = new StringBuffer();
        for (int j = 0; j < chars.length; j++) {
            if (j == 0) {
                target.append(chars[j].toLowerCase());
            } else {
                if (chars[j - 1].equals("_")) {
                    target.append(chars[j].toUpperCase());
                } else if (!chars[j].equals("_"))
                    target.append(chars[j].toLowerCase());
            }
        }
        return target.toString();
    }

    /**
     * @param date java.util.date
     * @return
     * @describe Date类型转字符串
     */
    public static String dateToString(LocalDate date) {
        if (date == null) {
            return EMPTY;
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * @param date java.util.date
     * @return
     * @author jiangyx
     * @describe 将长时间格式时间转换为字符串 yyyy-MM-dd HH:mm:ss
     */
    public static String dateToStrLong(LocalDateTime date) {
        if (date == null) {
            return EMPTY;
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * <p>
     * clean illegal char
     * </p>
     *
     * @param str cleaned object
     * @return
     */
    public static String celanIllegalChar(String str) {
        if (str != null) {
            Pattern pattern = Pattern.compile("[\t\r\n]");
            Matcher m = pattern.matcher(str);
            return m.replaceAll("");
        }
        return str;
    }

    /**
     * @param str
     * @describe 转驼峰
     */
    public static String strTransHump(String str) {
        String[] split = str.split("_");
        StringBuilder resBuilder = new StringBuilder(split[0].toLowerCase());
        for (int i = 1; i < split.length; i++) {
            resBuilder.append(split[i].substring(0, 1).toUpperCase()).append(split[i].substring(1).toLowerCase());
        }
        return resBuilder.toString();
    }

    /**
     * 过滤特殊字符
     *
     * @param str
     * @return
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s*");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

    /**
     * 下划线转驼峰
     */
    public static String lineToHump(String str) {
        StringBuffer sb = new StringBuffer();
        if(!ObjectUtils.isEmpty(str)) {
            str = str.toLowerCase();
            if (str.contains("_")) {
                Matcher matcher = linePattern.matcher(str);
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

    //只把下斜线变成驼峰，下斜线后的字母大小写不变
    public static String lineToHumpOthersNoChange(String str) {
        StringBuffer sb = new StringBuffer();
        if (str.contains("_")){
            
            Matcher matcher = linePattern.matcher(str);
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
            }
            matcher.appendTail(sb);
        }else{
            sb.append(str);
        }
        return sb.toString();
    }


    /**
     * 驼峰转下划线
     */
    public static String humpToLine(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public static String nvl(String s, String d) {
        return (s == null) ? d : s.trim();
    }

    public static String nvl(String s) {
        return (s == null) ? EMPTY : s.trim();
    }

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

    public static String buildCodeName(String code, String name) {
        return buildCodeName(code, name, " ");
    }

    public static String buildCodeName(String code, String name, String split) {
        StringBuffer content = new StringBuffer();
        if (isNotEmpty(code)) {
            if (isEmpty(split)) split = " ";
            content.append(code).append(split);
        }

        if (isNotEmpty(name)) {
            content.append(name);
        }
        return content.toString();
    }

    public static String urlDecode(String str) {
        try {
            str = URLDecoder.decode(str, PubCommonConst.UTF_8);
        } catch (Exception e) {
            log.debug("URL decode failed", e);
        }
        return str;
    }

    public static String urlEncode(String str) {
        try {
            str = URLEncoder.encode(str, PubCommonConst.UTF_8);
        } catch (UnsupportedEncodingException e) {
            log.debug("URL encode failed", e);
        }
        return str;
    }


    public static String urlEncodeFielName(String fileName) {
        String fileNamePrefix;
        String fileNameSuffix;
        if (fileName.lastIndexOf(".") > 0) {
            fileNamePrefix = fileName.substring(0, fileName.lastIndexOf("."));
            fileNameSuffix = fileName.replaceAll(fileNamePrefix, "");
        } else {
            fileNamePrefix = fileName;
            fileNameSuffix = "";
        }
        return urlEncode(fileNamePrefix) + fileNameSuffix;
    }

    /**
     *
     * @param strs
     * @param limit
     * @return
     */
    public static String formatStrList(List<String> strs,Integer limit) {
        return formatStrList(strs, COMMA,limit);

    }

    /**
     * 使用分隔符，格式化字符串集合
     * @param strs
     * @param separator
     * @param limit
     * @return
     */
    public static String formatStrList( List<String> strs, String separator, Integer limit) {
        if (isEmpty(separator)) separator = COMMA;
        StringBuffer res = new StringBuffer();
        if (PeachCollectionUtil.isNotEmpty(strs)) {
            for (int i = 0; i < strs.size(); i++) {
                String s = strs.get(i);
                if (limit != 0 && i == limit){
                    res.append(" ...");
                }else{
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
     * 将传入的值，以String的形式返回
     * 可处理 BigDecimal，Integer，String 三种类型
     *
     * @param value
     * @return
     */
    public static String getStringValue( Object value ) {
        return getStringValue(value, EMPTY);
    }

    /**
     * 将传入的值，以String的形式返回
     * 可处理 BigDecimal，Integer，String 三种类型
     * @param value
     * @return
     */
    public static String getStringValue( Object value,String defalutValue ) {
        String finalValue = defalutValue;
        try {
            if (null != value) {
                if (value instanceof BigDecimal temp) {
                    finalValue = temp.toString();
                } else if (value instanceof Integer temp) {
                    finalValue = temp.toString();
                } else if (value instanceof String) {
                    finalValue = value.toString();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return finalValue;
    }


}
