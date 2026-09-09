package com.peach.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 日期时间工具类（基于 JDK 21 / java.time 深度重构版）
 * <p>
 * 提供基于 {@link java.time} 线程安全 API 的格式化、解析、计算与比较功能，
 * 保持完全兼容原有方法，同时提升性能与扩展性。
 * </p>
 *
 * @author Mr Shu
 * @version 2.2.0
 * @since 2024/10/10
 */
public final class DateUtil {

    private static final Logger log = LoggerFactory.getLogger(DateUtil.class);

    private DateUtil() {
        throw new IllegalStateException("Utility class");
    }

    /* ==================================== 日期时间格式常量 ==================================== */

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_ONLY_PATTERN = "HH:mm:ss";
    public static final String CN_DATE_PATTERN = "yyyy年MM月dd日";
    public static final String MONTH_PATTERN = "yyyy-MM";
    public static final String TIMESTAMP_PATTERN = "yyyyMMddHHmmssSSS";
    public static final String DATE_PATTERN_COMPACT_DATETIME = "yyyyMMddHHmmss";
    public static final String DATE_PATTERN_COMPACT_DATE = "yyyyMMdd";

    /* ==================================== 线程安全格式化器 ==================================== */

    public static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    public static final DateTimeFormatter TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern(TIME_ONLY_PATTERN);
    public static final DateTimeFormatter CN_DATE_FORMATTER = DateTimeFormatter.ofPattern(CN_DATE_PATTERN);
    public static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern(MONTH_PATTERN);
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);

    // JDK 21 性能优化：静态预编译紧凑格式 Formatter，避免多次调用 ofPattern 重复创建
    public static final DateTimeFormatter COMPACT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN_COMPACT_DATETIME);

    /* ==================================== 预编译正则表达式 ==================================== */

    private static final Pattern PATTERN_TIME_ONLY = Pattern.compile("^\\d{2}\\s*:\\s*\\d{2}(\\s*:\\s*\\d{2})?$");
    private static final Pattern PATTERN_MONTH_DAY = Pattern.compile("^\\d{1,2}\\D+\\d{1,2}$");
    private static final Pattern PATTERN_NON_DIGIT = Pattern.compile("\\D+");

    /* ==================================== 基础获取与当前时间 ==================================== */

    /**
     * 获取当前标准格式时间字符串 (yyyy-MM-dd HH:mm:ss)
     *
     * @return 当前时间字符串
     */
    public static String nowTime() {
        return LocalDateTime.now(Clock.systemDefaultZone()).format(LOCAL_DATE_TIME_FORMATTER);
    }

    /**
     * 获取当前标准格式日期字符串 (yyyy-MM-dd)
     *
     * @return 当前日期字符串
     */
    public static String nowDate() {
        return LocalDate.now(Clock.systemDefaultZone()).format(LOCAL_DATE_FORMATTER);
    }

    /**
     * 获取当前日期字符串的别名方法 (yyyy-MM-dd)
     *
     * @return 当前日期字符串
     */
    public static String getCurDate() {
        return nowDate();
    }

    /**
     * 获取当前年份
     *
     * @return 当前年份整数
     */
    public static int getCurYear() {
        return LocalDate.now(Clock.systemDefaultZone()).getYear();
    }

    /**
     * 获取当前月份 (1-12)
     *
     * @return 当前月份整数
     */
    public static int getCurMonth() {
        return LocalDate.now(Clock.systemDefaultZone()).getMonthValue();
    }

    /**
     * 获取带有毫秒的时间戳字符串 (yyyyMMddHHmmssSSS)
     *
     * @return 毫秒级时间戳字符串
     */
    public static String getTimeStamp() {
        return LocalDateTime.now(Clock.systemDefaultZone()).format(TIMESTAMP_FORMATTER);
    }

    /* ==================================== 格式化 (Format) ==================================== */

    /**
     * 格式化 LocalDateTime 为标准时间字符串 (yyyy-MM-dd HH:mm:ss)
     *
     * @param dateTime 时间对象
     * @return 格式化后的时间字符串
     */
    public static String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(LOCAL_DATE_TIME_FORMATTER);
    }

    /**
     * 解析时间字符串并重新格式化为标准时间格式 (yyyy-MM-dd HH:mm:ss)
     *
     * @param param 时间字符串
     * @return 格式化后的时间字符串
     */
    public static String formatTime(String param) {
        LocalDateTime dateTime = parseLocalDateTime(param);
        return dateTime == null ? "" : dateTime.format(LOCAL_DATE_TIME_FORMATTER);
    }

    /**
     * 格式化 LocalDate 为标准日期字符串 (yyyy-MM-dd)
     *
     * @param date 日期对象
     * @return 格式化后的日期字符串
     */
    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(LOCAL_DATE_FORMATTER);
    }

    /**
     * 解析日期字符串并重新格式化为标准日期格式 (yyyy-MM-dd)
     *
     * @param param 日期字符串
     * @return 格式化后的日期字符串
     */
    public static String formatDate(String param) {
        LocalDate date = parseLocalDate(param);
        return date == null ? "" : date.format(LOCAL_DATE_FORMATTER);
    }

    /**
     * 指定年月日生成标准格式日期字符串 (yyyy-MM-dd)
     *
     * @param year  年份
     * @param month 月份
     * @param day   日期
     * @return 格式化后的日期字符串
     */
    public static String formatDate(Integer year, Integer month, Integer day) {
        if (year == null || month == null || day == null) {
            return "";
        }
        return LocalDate.of(year, month, day).format(LOCAL_DATE_FORMATTER);
    }

    /**
     * 格式化 LocalDate 为月份字符串 (yyyy-MM)
     *
     * @param date 日期对象
     * @return 格式化后的月份字符串
     */
    public static String formatMonth(LocalDate date) {
        return date == null ? "" : date.format(MONTH_FORMATTER);
    }

    /**
     * 格式化为中文日期格式 (yyyy年MM月dd日)
     *
     * @param date 日期对象
     * @return 中文格式日期字符串
     */
    public static String formatCnDate(LocalDate date) {
        return date == null ? "" : date.format(CN_DATE_FORMATTER);
    }

    /**
     * 解析日期字符串并格式化为中文日期格式 (yyyy年MM月dd日)
     *
     * @param param 日期字符串
     * @return 中文格式日期字符串
     */
    public static String formatCnDate(String param) {
        LocalDate date = parseLocalDate(param);
        return date == null ? "" : date.format(CN_DATE_FORMATTER);
    }


    /* ==================================== 解析 (Parse) ==================================== */

    /**
     * 解析字符串为 LocalDateTime (支持标准格式 yyyy-MM-dd HH:mm:ss)
     *
     * @param strDate 时间字符串
     * @return LocalDateTime 对象
     */
    public static LocalDateTime parseLocalDateTime(String strDate) {
        if (StringUtil.isEmpty(strDate)) {
            return null;
        }
        try {
            return LocalDateTime.parse(strDate.trim(), LOCAL_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid datetime string: " + strDate, ex);
        }
    }

    /**
     * 解析字符串为 LocalDate (支持标准格式 yyyy-MM-dd)
     *
     * @param strDate 日期字符串
     * @return LocalDate 对象
     */
    public static LocalDate parseLocalDate(String strDate) {
        if (StringUtil.isEmpty(strDate)) {
            return null;
        }
        try {
            return LocalDate.parse(strDate.trim(), LOCAL_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date string: " + strDate, ex);
        }
    }

    /* ==================================== 日期属性提取 ==================================== */

    /**
     * 获取日期中的月份字符串
     *
     * @param strDate 日期字符串
     * @return 月份字符串 (1-12)
     */
    public static String getMonth(String strDate) {
        LocalDate date = parseLocalDate(strDate);
        return date == null ? null : String.valueOf(date.getMonthValue());
    }

    /**
     * 获取日期中的年份字符串
     *
     * @param strDate 日期字符串
     * @return 年份字符串
     */
    public static String getYear(String strDate) {
        LocalDate date = parseLocalDate(strDate);
        return date == null ? null : String.valueOf(date.getYear());
    }

    /**
     * 获取日期中的日（当月的第几天）
     *
     * @param strDate 日期字符串
     * @return 日字符串
     */
    public static String getDayOfMonth(String strDate) {
        try {
            LocalDate date = parseLocalDate(strDate);
            return date == null ? null : String.valueOf(date.getDayOfMonth());
        } catch (Exception e) {
            log.debug("Failed to resolve day of month from date: {}", strDate, e);
            return StringUtil.EMPTY;
        }
    }

    /**
     * 根据出生日期计算当前年龄
     *
     * @param birthDate 出生日期
     * @return 年龄（整数）
     */
    public static int getAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return (int) ChronoUnit.YEARS.between(birthDate, LocalDate.now(Clock.systemDefaultZone()));
    }

    /**
     * 获取上个月的月份字符串 (yyyy-MM)
     *
     * @return 上个月字符串
     */
    public static String getLastMonth() {
        return YearMonth.now(Clock.systemDefaultZone()).minusMonths(1).format(MONTH_FORMATTER);
    }

    /* ==================================== 业务计算与比较 ==================================== */

    /**
     * 比较两个日期字符串的大小
     *
     * @param d1 日期1 (yyyy-MM-dd)
     * @param d2 日期2 (yyyy-MM-dd)
     * @return 0: 相等; &lt;0: d1 在 d2 之前; &gt;0: d1 在 d2 之后
     */
    public static int compareDate(String d1, String d2) {
        if (Objects.equals(d1, d2)) {
            return 0;
        }
        if (d1 == null) {
            return -1;
        }
        if (d2 == null) {
            return 1;
        }
        LocalDate c1 = parseLocalDate(d1);
        LocalDate c2 = parseLocalDate(d2);
        if (c1 == null || c2 == null) {
            throw new IllegalArgumentException("Invalid date for comparison");
        }
        return c1.compareTo(c2);
    }

    /**
     * 获取指定年月的第一天 (yyyy-MM-01)
     *
     * @param year  年份字符串
     * @param month 月份字符串
     * @return 格式化后的第一天日期字符串
     */
    public static String getFirstDayOfMonth(String year, String month) {
        YearMonth ym = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
        return ym.atDay(1).format(LOCAL_DATE_FORMATTER);
    }

    /**
     * 获取指定年月的最后一天
     *
     * @param year  年份字符串
     * @param month 月份字符串
     * @return 格式化后的最后一天日期字符串
     */
    public static String getLastDayOfMonth(String year, String month) {
        YearMonth ym = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
        return ym.atEndOfMonth().format(LOCAL_DATE_FORMATTER);
    }

    /**
     * 计算两个日期相差的天数
     *
     * @param beginDate 起始日期
     * @param endDate   结束日期
     * @return 相差天数
     */
    public static Long daysBetween(LocalDate beginDate, LocalDate endDate) {
        if (beginDate == null || endDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(beginDate, endDate);
    }


    /**
     * 为给定的 LocalDateTime 增加/减少天数
     *
     * @param dateTime 原时间
     * @param days     增加的天数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        return dateTime == null ? null : dateTime.plusDays(days);
    }

    /**
     * 为给定的 LocalDateTime 增加/减少分钟数
     *
     * @param dateTime 原时间
     * @param minutes  增加的分钟数（可为负数）
     * @return 计算后的 LocalDateTime
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime == null ? null : dateTime.plusMinutes(minutes);
    }

    /**
     * 判断时间字符串是否为合法的标准时间格式 (yyyy-MM-dd HH:mm:ss)
     *
     * @param dateStr 时间字符串
     * @return 是否有效
     */
    public static boolean isValidDate(String dateStr) {
        if (StringUtil.isEmpty(dateStr)) {
            return false;
        }
        try {
            LocalDateTime.parse(dateStr.trim(), LOCAL_DATE_TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 校验日期列表是否为严格升序排列
     *
     * @param dates 日期字符串列表
     * @return 是否升序
     */
    public static boolean isSorted(List<String> dates) {
        if (dates == null || dates.size() <= 1) {
            return true;
        }
        for (int i = 1; i < dates.size(); i++) {
            if (compareDate(dates.get(i - 1), dates.get(i)) >= 0) {
                return false;
            }
        }
        return true;
    }


    /**
     * 给定日期按月增加/减少
     *
     * @param date        原日期字符串
     * @param datePattern 日期格式
     * @param months      增加的月份数
     * @return 计算后的日期字符串
     */
    public static String addMonth(String date, String datePattern, int months) {
        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern(datePattern);
            LocalDate parsed = LocalDate.parse(date, format);
            return parsed.plusMonths(months).format(format);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + date, e);
        }
    }

    /**
     * 转换紧凑格式日期时间 (yyyyMMddHHmmss) 为标准时间字符串 (yyyy-MM-dd HH:mm:ss)
     *
     * @param dateStr 紧凑日期时间字符串
     * @return 标准时间字符串
     */
    public static String convertCompactToDateTime(String dateStr) {
        LocalDateTime ldt = LocalDateTime.parse(dateStr, COMPACT_DATETIME_FORMATTER);
        return ldt.format(LOCAL_DATE_TIME_FORMATTER);
    }

    /**
     * 转换标准时间字符串 (yyyy-MM-dd HH:mm:ss) 为紧凑格式 (yyyyMMddHHmmss)
     *
     * @param dateTimeStr 标准时间字符串
     * @return 紧凑时间字符串
     */
    public static String convertStandardToCompact(String dateTimeStr) {
        LocalDateTime ldt = parseLocalDateTime(dateTimeStr);
        if (ldt == null) {
            return "";
        }
        return ldt.format(COMPACT_DATETIME_FORMATTER);
    }


    /**
     * 智能识别并格式化任意不规则日期字符串为 yyyy-MM-dd
     *
     * @param dateStr 任意格式日期字符串
     * @return 格式化后的 yyyy-MM-dd 日期字符串
     */
    public static String formatAnyDate(String dateStr) {
        if (StringUtil.isEmpty(dateStr)) {
            throw new IllegalArgumentException("Date string must not be empty");
        }

        String working = dateStr.trim();
        String curDate = nowDate();

        // 补全仅时间或仅月日的情况
        if (PATTERN_TIME_ONLY.matcher(working).matches()) {
            working = curDate + " " + working;
        } else if (PATTERN_MONTH_DAY.matcher(working).matches()) {
            working = curDate.substring(0, 4) + "-" + working;
        }

        // 清理所有非数字字符替换为统一分隔符 (使用预编译 Pattern 提升 JDK 21 正则匹配性能)
        String cleanDate = PATTERN_NON_DIGIT.matcher(working).replaceAll("-");

        // 移除前后可能因替换产生的多余连字符
        if (cleanDate.startsWith("-")) {
            cleanDate = cleanDate.substring(1);
        }
        if (cleanDate.endsWith("-")) {
            cleanDate = cleanDate.substring(0, cleanDate.length() - 1);
        }

        String[] parts = cleanDate.split("-");

        // 根据数字段尝试构造 LocalDate
        try {
            if (parts.length >= 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day).format(LOCAL_DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to parse flexible date: {}", dateStr, e);
        }

        throw new IllegalArgumentException("Invalid date format: " + dateStr);
    }

    /**
     * 兼容线程池场景的历史清理入口；java.time 格式化器无 ThreadLocal 状态，此方法为 no-op。
     */
    public static void clearThreadLocalState() {
        // no-op after java.time migration
    }
}
