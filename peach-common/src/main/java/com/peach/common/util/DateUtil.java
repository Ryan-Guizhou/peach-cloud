package com.peach.common.util;

import java.time.Clock;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 时间格式化工具类；基于 {@link java.time}，新代码优先使用本类或直接使用 java.time API。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2024/10/10
 */
@Slf4j
public final class DateUtil {

    private DateUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static final String DATA_PATTERN = "yyyy-MM-dd";

    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 仅时间部分格式（Jackson Sa-Token 会话等场景）。
     */
    public static final String TIME_ONLY_PATTERN = "HH:mm:ss";

    /**
     * {@link #TIME_ONLY_PATTERN} 对应的格式化器。
     */
    public static final DateTimeFormatter TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern(TIME_ONLY_PATTERN);

    /**
     * {@link #DATA_PATTERN} 对应的格式化器。
     */
    public static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATA_PATTERN);

    /**
     * {@link #TIME_PATTERN} 对应的格式化器。
     */
    public static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);

    public static final String DATE_FORMAT1 = "yyyy-MM-dd-HH-mm-ss";
    public static final String DATE_PATTERN2 = "yyyy-MM-dd-HH-mm";
    public static final String DATE_PATTERN3 = "yyyy-MM-dd-HH";
    public static final String DATE_PATTERN4 = "yyyy-MM-dd";
    public static final String DATE_PATTERN5 = "yyyy-MM";
    public static final String DATE_PATTERN6 = "yyyy";
    public static final String DATE_PATTERN7 = "yyyyMMddHHmmss";
    public static final String DATE_PATTERN8 = "yyyyMMddHHmm";
    public static final String DATE_PATTERN9 = "yyyyMMddHH";
    public static final String DATE_PATTERN10 = "yyyyMMdd";
    public static final String DATE_PATTERN11 = "yyyyMM";
    public static final String DATE_PATTERN12 = "yyyy-dd-MM";
    public static final String DATE_PATTERN13 = "dd-MM-yyyy";
    public static final String CN_DATE_PATTERN = "yyyy年MM月dd日";

    private static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern(TIME_PATTERN);
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern(DATA_PATTERN);
    private static final DateTimeFormatter FORMAT_CN_DATE = DateTimeFormatter.ofPattern(CN_DATE_PATTERN);
    private static final DateTimeFormatter FORMAT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static String nowTime() {
        return LocalDateTime.now(Clock.systemDefaultZone()).format(FORMAT_TIME);
    }

    public static String nowDate() {
        return LocalDate.now(Clock.systemDefaultZone()).format(FORMAT_DATE);
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(FORMAT_TIME);
    }

    public static String formtCNDate(String param) {
        LocalDate date = parseLocalDate(param);
        return date == null ? "" : date.format(FORMAT_CN_DATE);
    }

    public static String formtCNDate(LocalDate date) {
        return date == null ? "" : date.format(FORMAT_CN_DATE);
    }

    public static String formatTime(String param) {
        LocalDateTime dateTime = parseLocalDateTime(param);
        return dateTime == null ? "" : dateTime.format(FORMAT_TIME);
    }

    public static String formatDate(String param) {
        LocalDate date = parseLocalDate(param);
        return date == null ? "" : date.format(FORMAT_DATE);
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(FORMAT_DATE);
    }

    public static String formatDate(Integer year, Integer month, Integer day) {
        return LocalDate.of(year, month, day).format(FORMAT_DATE);
    }

    public static LocalDateTime parseLocalDateTime(String strDate) {
        if (StringUtil.isEmpty(strDate)) {
            return null;
        }
        try {
            return LocalDateTime.parse(strDate, FORMAT_TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid datetime: " + strDate, ex);
        }
    }

    public static LocalDate parseLocalDate(String strDate) {
        if (StringUtil.isEmpty(strDate)) {
            return null;
        }
        try {
            return LocalDate.parse(strDate, FORMAT_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date: " + strDate, ex);
        }
    }

    public static String getCurDate() {
        return nowDate();
    }

    public static String getMonth(String strDate) {
        LocalDate date = parseLocalDate(strDate);
        return date == null ? null : String.valueOf(date.getMonthValue());
    }

    public static String getYear(String strDate) {
        LocalDate date = parseLocalDate(strDate);
        return date == null ? null : String.valueOf(date.getYear());
    }

    public static String getDayOfMonth(String strDate) {
        try {
            LocalDate date = parseLocalDate(strDate);
            return date == null ? null : String.valueOf(date.getDayOfMonth());
        } catch (Exception e) {
            log.debug("Failed to resolve day of month from date: {}", strDate, e);
            return StringUtil.EMPTY;
        }
    }

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

    public static String getLastDayOfMonth(String year, String month) {
        LocalDate first = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);
        return first.withDayOfMonth(first.lengthOfMonth()).format(FORMAT_DATE);
    }

    public static String getFirstDayOfMonth(String year, String month) {
        int monthValue = Integer.parseInt(month);
        String monthText = monthValue > 9 ? month : "0" + month;
        return year + "-" + monthText + "-01";
    }

    public static int getCurYear() {
        return LocalDate.now(Clock.systemDefaultZone()).getYear();
    }

    public static int getCurMonth() {
        return LocalDate.now(Clock.systemDefaultZone()).getMonthValue();
    }

    public static boolean isValidDate(String dateStr) {
        try {
            LocalDateTime.parse(dateStr, FORMAT_TIME);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static LocalDateTime paseLocalDateTime(String date) {
        return parseLocalDateTime(date);
    }

    public static LocalDateTime paseLocalDateTime(LocalDateTime dateTime) {
        return dateTime;
    }

    public static LocalDate paseLocalDate(String date) {
        return parseLocalDate(date);
    }

    public static LocalDate paseLocalDate(LocalDate date) {
        return date;
    }

    public static Long daysBetDates(LocalDate beginDate, LocalDate endDate) {
        if (beginDate == null || endDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(beginDate, endDate);
    }

    public static String formatLocalDate(LocalDate date) {
        return date == null ? null : date.format(FORMAT_DATE);
    }

    public static String formatLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.format(FORMAT_TIME);
    }

    public static String getTimeStamp() {
        return LocalDateTime.now(Clock.systemDefaultZone()).format(FORMAT_TIMESTAMP);
    }

    public static String formatAnyDate(String dateStr) {
        Map<String, String> dateRegFormat = new HashMap<>();
        dateRegFormat.put("^\\d{4}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D*$", DATE_FORMAT1);
        dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}$", DATE_PATTERN2);
        dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}$", DATE_PATTERN3);
        dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}$", DATE_PATTERN4);
        dateRegFormat.put("^\\d{4}\\D+\\d{2}$", DATE_PATTERN5);
        dateRegFormat.put("^\\d{4}$", DATE_PATTERN6);
        dateRegFormat.put("^\\d{14}$", DATE_PATTERN7);
        dateRegFormat.put("^\\d{12}$", DATE_PATTERN8);
        dateRegFormat.put("^\\d{10}$", DATE_PATTERN9);
        dateRegFormat.put("^\\d{8}$", DATE_PATTERN10);
        dateRegFormat.put("^\\d{6}$", DATE_PATTERN11);
        dateRegFormat.put("^\\d{2}\\s*:\\s*\\d{2}\\s*:\\s*\\d{2}$", DATE_FORMAT1);
        dateRegFormat.put("^\\d{2}\\s*:\\s*\\d{2}$", DATE_PATTERN2);
        dateRegFormat.put("^\\d{2}\\D+\\d{1,2}\\D+\\d{1,2}$", DATA_PATTERN);
        dateRegFormat.put("^\\d{1,2}\\D+\\d{1,2}$", DATE_PATTERN12);
        dateRegFormat.put("^\\d{1,2}\\D+\\d{1,2}\\D+\\d{4}$", DATE_PATTERN13);

        String curDate = nowDate();
        String strSuccess = "";
        for (Map.Entry<String, String> entry : dateRegFormat.entrySet()) {
            String pattern = entry.getKey();
            if (Pattern.compile(pattern).matcher(dateStr).matches()) {
                String working = dateStr;
                if ("^\\d{2}\\s*:\\s*\\d{2}\\s*:\\s*\\d{2}$".equals(pattern)
                        || "^\\d{2}\\s*:\\s*\\d{2}$".equals(pattern)) {
                    working = new StringBuilder(curDate).append("-").append(dateStr).toString();
                } else if ("^\\d{1,2}\\D+\\d{1,2}$".equals(pattern)) {
                    working = new StringBuilder(curDate.substring(0, 4)).append("-").append(dateStr).toString();
                }
                String dateReplace = working.replaceAll("\\D+", "-");
                DateTimeFormatter parser = DateTimeFormatter.ofPattern(entry.getValue());
                LocalDate parsed = LocalDate.parse(dateReplace, parser);
                strSuccess = parsed.format(FORMAT_DATE);
                break;
            }
        }
        if (strSuccess.isEmpty()) {
            throw new IllegalArgumentException("日期格式无效" + dateStr);
        }
        return strSuccess;
    }

    /**
     * 兼容线程池场景的历史清理入口；java.time 格式化器无 ThreadLocal 状态，此方法为 no-op。
     */
    public static void clearThreadLocalState() {
        // no-op after java.time migration
    }

    public static boolean isSort(List<String> dates) {
        for (int i = 1; i < dates.size(); i++) {
            if (compareDate(dates.get(i - 1), dates.get(i)) >= 0) {
                return false;
            }
        }
        return true;
    }

    public static LocalDate toLocalDate(LocalDate date) {
        return date;
    }

    public static String getDataPattern(String dateStr) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_PATTERN7);
        LocalDateTime ldt = LocalDateTime.parse(dateStr, dtf);
        return ldt.format(FORMAT_TIME);
    }

    public static String addMonth(String date, String dateType, int months) {
        try {
            DateTimeFormatter format = DateTimeFormatter.ofPattern(dateType);
            LocalDate parsed = LocalDate.parse(date, format);
            return parsed.plusMonths(months).format(format);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式无效" + date, e);
        }
    }
}
