package com.peach.common.util;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // 时间格式化工具类
 * @CreateTime 2024/10/10 15:22
 */
public class DateUtil {

    public static final String DATA_PATTERN = "yyyy-MM-dd";

    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

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

    private static final ThreadLocal<DateFormat> TIME_LOCAL = ThreadLocal
            .withInitial(() -> new SimpleDateFormat(TIME_PATTERN));

    private static final ThreadLocal<DateFormat> DATA_LOCAL = ThreadLocal
            .withInitial(() -> new SimpleDateFormat(DATA_PATTERN));

    private static final ThreadLocal<DateFormat> CN_DATA_LOCAL = ThreadLocal
            .withInitial(() -> new SimpleDateFormat(CN_DATE_PATTERN));

    private static final DateTimeFormatter FORMAT_LOCAL_DATE = DateTimeFormatter.ofPattern(DATA_PATTERN);

    private static final DateTimeFormatter FORMAT_LOCAL_TIME = DateTimeFormatter.ofPattern(TIME_PATTERN);

    public static String nowTime() {
        try {
            return TIME_LOCAL.get().format(new Date());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String nowDate() {
        try {
            return DATA_LOCAL.get().format(new Date());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @功能 将时间进行格式化 yyyy-MM-dd HH:mm:ss
     * @param date
     * @throws ParseException
     */
    public static String formatTime(Date date) {
        try {
            if (date != null) {
                return TIME_LOCAL.get().format(date);
            } else {
                return "";
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 格式化日期
     * yyyy年MM月dd日
     * @param param
     * @return
     */
    public static String formtCNDate(String param) {
        try {
            return formtCNDate(parseDate(param));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 格式化日期
     * yyyy年MM月dd日
     * @param date
     * @return
     */
    public static String formtCNDate(Date date) {
        try {
            if (date != null) {
                return CN_DATA_LOCAL.get().format(date);
            } else {
                return "";
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 格式化日期
     *
     * @param param
     * @return
     */
    public static String formatTime(String param) {
        try {
            Date date = parseTime(param);
            return TIME_LOCAL.get().format(date);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @功能 将日期进行格式化 yyyy-MM-dd
     * @param param
     * @throws ParseException
     */
    public static String formatDate(String param) {
        try {
            Date date = parseDate(param);
            return DATA_LOCAL.get().format(date);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @功能 将日期进行格式化 yyyy-MM-dd
     * @param date
     * @throws ParseException
     */
    public static String formatDate(Date date) {
        try {
            if (date != null) {
                return DATA_LOCAL.get().format(date);
            } else {
                return "";
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @功能 将日期进行格式化 yyyy-MM-dd
     * @param year  年
     * @param month 月
     * @param day   日
     * @throws ParseException
     */
    public static String formatDate(Integer year, Integer month, Integer day) {
        String date;
        try {
            StringBuilder res = new StringBuilder();
            if (month > 9) {
                res.append(year).append("-").append(month).append("-").append(day);
            } else {
                res.append(year).append("-").append("0").append(month).append("-").append(day);
            }
            date = res.toString();
        } catch (Exception ex) {
            date = getCurDate();
            throw new RuntimeException(ex);
        }
        return date;
    }

    /**
     *
     * @功能 将字符串转化为时间 yyyy-MM-dd HH:mm:ss
     * @param strDate
     * @return
     * @throws ParseException
     */
    public static Date parseTime(String strDate) {
        try {
            return TIME_LOCAL.get().parse(strDate);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @功能 将字符串转化为日期 yyyy-MM-dd
     * @param strDate
     * @return
     * @throws ParseException
     */
    public static Date parseDate(String strDate) {
        if (StringUtil.isEmpty(strDate)) {
            return null;
        }
        try {
            return DATA_LOCAL.get().parse(strDate);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @功能 将当前系统日期转化成yyyy-MM-dd格式字符串
     * @return
     * @throws ParseException
     */
    public static String getCurDate() {
        String res = StringUtil.EMPTY;
        try {
            res = DATA_LOCAL.get().format(new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     *
     * @功能 将当前系统日期转化成指定格式字符串
     * @param pattern
     * @return
     * @throws ParseException
     */
    /**
     * 20170902 tianlei SimpleDateFormat每次实例化耗费资源，不建议此方式；一个系统一般也就只有几种时间格式，明确下来即可
     */
    @Deprecated
    public static String getCurDate(String pattern) {
        String res = StringUtil.EMPTY;
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            res = format.format(new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 根据日期字符串获取月份
     */
    public static String getMonth(String strDate) {
        if (StringUtil.isEmpty(strDate)) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(parseDate(strDate));
        int month = cal.get(Calendar.MONTH);
        return String.valueOf(month + 1);
    }

    /**
     * 根据日期字符串获取年度
     */
    public static String getYear(String strDate) {
        if (StringUtil.isEmpty(strDate)) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(parseDate(strDate));
        int year = cal.get(Calendar.YEAR);
        return String.valueOf(year);
    }

    /**
     * 根据日期字符串获取日期
     */
    public static String getDayOfMonth(String strDate) {
        String res = StringUtil.EMPTY;
        try {
            if (StringUtil.isEmpty(strDate)) {
                return null;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(parseDate(strDate));
            int day = cal.get(Calendar.DATE);
            res = String.valueOf(day);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 比较两个日期
     *
     * @throws ParseException
     * @see # d1小于d2, 返回-1
     * @see # d1等于d2, 返回0
     * @see # d1大于d2, 返回1
     */
    @SuppressWarnings("unchecked")
    public static int compareDate(String d1, String d2) throws ParseException {
        if (d1 == d2) {
            return 0;
        }
        if (null == d1) {
            return -1;
        }
        if (null == d2) {
            return 1;
        }
        if (d1.equals(d2)) {
            return 0;
        }
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(parseDate(d1));
        c2.setTime(parseDate(d2));
        int result = ((Comparable) c1).compareTo(c2);
        if (result > 0) {
            return 1;
        } else if (result < 0) {
            return -1;
        }
        return 0;
    }

    /**
     * 获取月末最后一天 todo shancong 原来的方法有问题，只能算对31天的月份。二月份和30天的月份，算的不对 不要什么都相信百度
     */
    public static String getLastDayOfMonth(String year, String month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, Integer.parseInt(year));
        calendar.set(Calendar.MONTH, Integer.parseInt(month) - 1);
        calendar.set(Calendar.DATE, 1);
        calendar.roll(Calendar.DATE, -1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DATE));
        return DATA_LOCAL.get().format(calendar.getTime());
    }

    /**
     * 获取月份第一天，哈哈，其实没什么用；废话，就是为了简化统一
     */
    public static String getFirstDayOfMonth(String year, String month) {
        return year + "-" + (Integer.parseInt(month) > 9 ? month : "0" + month) + "-" + "01";
    }

    /**
     * 获取当前年度
     */
    public static int getCurYear() {
        return Calendar.getInstance().get(Calendar.YEAR);

    }

    /**
     * 获取当前月份
     */
    public static int getCurMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;

    }

    /**
     * 判断日期型字符串是否合法
     */
    public static boolean isValidDate(String dateStr) {
        boolean b = false;
        try {
            DATA_LOCAL.get().setLenient(false);
            DATA_LOCAL.get().parse(dateStr);
            b = true;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     *
     * @功能 将字符串转化为时间 yyyy-MM-dd HH:mm:ss
     * @param date
     * @return
     * @throws ParseException
     */
    public static LocalDateTime paseLocalDateTime(String date) {
        Date realTime = parseTime(date);
        return paseLocalDateTime(realTime);
    }

    /**
     *
     * @功能 LocalDateTime
     * @param date
     * @return
     * @throws ParseException
     */
    public static LocalDateTime paseLocalDateTime(Date date) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime localDateTime = date.toInstant().atZone(zoneId).toLocalDateTime();
        return localDateTime;
    }

    /**
     *
     * @功能 将字符串转化为日期 yyyy-MM-dd
     * @param date
     * @return
     * @throws ParseException
     */
    public static LocalDate paseLocalDate(String date) {
        Date realDate = parseDate(date);
        return paseLocalDate(realDate);
    }

    /**
     *
     * @功能 将Date转化为LocalDate
     * @param date
     * @return
     * @throws ParseException
     */
    public static LocalDate paseLocalDate(Date date) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate localDate = date.toInstant().atZone(zoneId).toLocalDate();
        return localDate;
    }

    /**
     * 计算天数差
     *
     * @param beginDate
     * @param endDate
     * @return
     */
    public static Long daysBetDates(Date beginDate, Date endDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate begin = beginDate.toInstant().atZone(zoneId).toLocalDate();
        LocalDate end = endDate.toInstant().atZone(zoneId).toLocalDate();
        Long days = end.toEpochDay() - begin.toEpochDay();
        return days;
    }

    public static String formatLocalDate(LocalDate date) {
        try {
            return FORMAT_LOCAL_DATE.format(date);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String formatLocalDateTime(LocalDateTime localDateTime) {
        try {
            return localDateTime.format(FORMAT_LOCAL_TIME);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 获取时间戳
     *
     * @return
     */
    public static String getTimeStamp() {
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyyMMddhhmmssSSS");
        String timeStamp = sdFormat.format(new Date());
        return timeStamp;
    }

    /**
     * @param dateStr 要匹配的字符串
     * @return 匹配成功返回 yyyy-MM-dd
     */
    public static String formatAnyDate(String dateStr) {

        HashMap<String, String> dateRegFormat = new HashMap<String, String>();
        dateRegFormat.put("^\\d{4}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D+\\d{1,2}\\D*$", DATE_FORMAT1); // 2018年3月12日
        // 13时5分34秒，-08-12
        // 12:05:34，2018/3/12
        // 12:5:34
        dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}$", DATE_PATTERN2); // 2018-08-12 12:05
        dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}\\D+\\d{2}$", DATE_PATTERN3); // 2018-08-12 12
        dateRegFormat.put("^\\d{4}\\D+\\d{2}\\D+\\d{2}$", DATE_PATTERN4); // 2018-08-12
        dateRegFormat.put("^\\d{4}\\D+\\d{2}$", DATE_PATTERN5); // 2018-08
        dateRegFormat.put("^\\d{4}$", DATE_PATTERN6); // 2018
        dateRegFormat.put("^\\d{14}$", DATE_PATTERN7); // 20180812120534
        dateRegFormat.put("^\\d{12}$", DATE_PATTERN8); // 201808121205
        dateRegFormat.put("^\\d{10}$", DATE_PATTERN9); // 2018081212
        dateRegFormat.put("^\\d{8}$", DATE_PATTERN10); // 20180812
        dateRegFormat.put("^\\d{6}$", DATE_PATTERN11); // 201808
        dateRegFormat.put("^\\d{2}\\s*:\\s*\\d{2}\\s*:\\s*\\d{2}$", DATE_FORMAT1); // 13:05:34 拼接当前日期
        dateRegFormat.put("^\\d{2}\\s*:\\s*\\d{2}$", DATE_PATTERN2); // 13:05 拼接当前日期
        dateRegFormat.put("^\\d{2}\\D+\\d{1,2}\\D+\\d{1,2}$", DATA_PATTERN); // 14.10.18(年.月.日)
        dateRegFormat.put("^\\d{1,2}\\D+\\d{1,2}$", DATE_PATTERN12); // 30.12(日.月) 拼接当前年份
        dateRegFormat.put("^\\d{1,2}\\D+\\d{1,2}\\D+\\d{4}$", DATE_PATTERN13); // 12.21.2018(日.月.年)

        String curDate = DATA_LOCAL.get().format(new Date());
        DateFormat formatter1 = DATA_LOCAL.get();
        DateFormat formatter2;
        String dateReplace;
        String strSuccess = "";
        try {
            for (String key : dateRegFormat.keySet()) {
                if (Pattern.compile(key).matcher(dateStr).matches()) {
                    formatter2 = new SimpleDateFormat(dateRegFormat.get(key));
                    if (key.equals("^\\d{2}\\s*:\\s*\\d{2}\\s*:\\s*\\d{2}$") || key.equals("^\\d{2}\\s*:\\s*\\d{2}$")) {// 13:05:34
                        // 或
                        // 13:05
                        // 拼接当前日期
                        dateStr = curDate + "-" + dateStr;
                    } else if (key.equals("^\\d{1,2}\\D+\\d{1,2}$")) {// 21.1 (日.月) 拼接当前年份
                        dateStr = curDate.substring(0, 4) + "-" + dateStr;
                    }
                    dateReplace = dateStr.replaceAll("\\D+", "-");
                    // System.out.println(dateRegExpArr[i]);
                    strSuccess = formatter1.format(formatter2.parse(dateReplace));
                    break;
                }
            }
        } catch (Exception e) {
            throw new Exception("日期格式无效" + dateStr);
        } finally {
            return strSuccess;
        }
    }

    /**
     *
     * 日期是否正序(且不可重复)
     *
     * @param dates
     * @return
     */
    public static boolean isSort(List<String> dates) {
        for (int i = 1; i < dates.size(); i++) {
            int j = 0;
            try {
                j = compareDate(dates.get(i - 1), dates.get(i));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (j >= 0) {
                return false;
            }
        }
        return true;
    }

    // java8 日期转换
    public static LocalDate toLocalDate(Date date) {
        if (null == date) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * yyyyMMddHHmmss 转换 yyyy-MM-dd HH:mm:ss
     */
    public static String getDataPattern(String dateStr) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_PATTERN7);
        LocalDateTime ldt = LocalDateTime.parse(dateStr, dtf);
        DateTimeFormatter fa = DateTimeFormatter.ofPattern(TIME_PATTERN);
        return ldt.format(fa);
    }

    /** 计算增加多少个月后的日期 */
    public static String addMonth(String date, String dateType, int months) {
        String nowDate = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat(dateType);
            Date parse = format.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parse);
            calendar.add(Calendar.MONTH, months);
            nowDate = format.format(calendar.getTime());
        } catch (ParseException e) {
            throw new RuntimeException("日期格式无效" + date);
        }
        return nowDate;
    }
}
