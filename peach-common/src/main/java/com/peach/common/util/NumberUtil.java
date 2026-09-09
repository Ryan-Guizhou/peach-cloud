package com.peach.common.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;


/**
 * 常用数字处理工具类（基于 JDK 21 重构版）
 * <p>
 * 涵盖 {@link BigDecimal}、{@link Long}、{@link Float}、{@link Double} 的空安全计算、
 * 类型安全转换、精度保留、千分位及百分比格式化与区间比较功能。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/14 15:51
 */
@Slf4j
public final class NumberUtil {

    /**
     * 默认保留小数位数：2 位
     */
    private static final int DEFAULT_SCALE = 2;

    /**
     * 默认舍入模式：四舍五入 (HALF_UP)
     */
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private NumberUtil() {
        throw new IllegalStateException("Utility class");
    }

    /* ==================================== 空安全与判断 (Null Safety & Predicates) ==================================== */

    /**
     * 判断 BigDecimal 是否为 null 或数值 0
     *
     * @param value 待检查的 BigDecimal 对象
     * @return true: 为 null 或 0; false: 非 0 数值
     */
    public static boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 判断 Long 是否为 null 或数值 0L
     *
     * @param value 待检查的 Long 对象
     * @return true: 为 null 或 0L; false: 非 0 数值
     */
    public static boolean isNullOrZero(Long value) {
        return value == null || value == 0L;
    }

    /**
     * 判断 Float 是否为 null 或数值 0.0f
     *
     * @param value 待检查的 Float 对象
     * @return true: 为 null 或 0.0f; false: 非 0 数值
     */
    public static boolean isNullOrZero(Float value) {
        return value == null || Float.compare(value, 0.0f) == 0;
    }

    /**
     * 安全转换 BigDecimal，若为 null 则返回 {@link BigDecimal#ZERO}
     *
     * @param value 待转换的 BigDecimal 对象
     * @return 转换后的 BigDecimal 对象（绝不为 null）
     */
    public static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 安全转换 Long，若为 null 则返回 0L
     *
     * @param value 待转换的 Long 对象
     * @return 转换后的 Long 值（绝不为 null）
     */
    public static Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 安全转换 Float，若为 null 则返回 0.0f
     *
     * @param value 待转换的 Float 对象
     * @return 转换后的 Float 值（绝不为 null）
     */
    public static Float nullToZero(Float value) {
        return value == null ? 0.0f : value;
    }

    /* ==================================== 类型转换 (Type Conversion) ==================================== */

    /**
     * 通用 Object 转换为 BigDecimal
     * <p>支持 String, Long, Integer, Double, Float 等常用数值类型，字符串解析会自动去除首尾空格。</p>
     *
     * @param value 待转换的对象
     * @return 转换后的 BigDecimal 对象；若入参为 null 或空字符串则返回 {@link BigDecimal#ZERO}
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            return trimmed.isEmpty() ? BigDecimal.ZERO : new BigDecimal(trimmed);
        }
        return new BigDecimal(String.valueOf(value));
    }

    /**
     * Long 转换为 BigDecimal
     *
     * @param value 待转换的 Long 对象
     * @return 转换后的 BigDecimal 对象；若入参为 null 则返回 {@link BigDecimal#ZERO}
     */
    public static BigDecimal toBigDecimal(Long value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    /**
     * Float 转换为 BigDecimal
     * <p>内部使用 {@link Float#toString(float)} 方式转换，规避直接二进制浮点转换引起的精度丢失风险。</p>
     *
     * @param value 待转换的 Float 对象
     * @return 转换后的 BigDecimal 对象；若入参为 null 则返回 {@link BigDecimal#ZERO}
     */
    public static BigDecimal toBigDecimal(Float value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(Float.toString(value));
    }

    /**
     * BigDecimal 安全转换为 Long（直接截断小数部分，向下取整）
     *
     * @param value 待转换的 BigDecimal 对象
     * @return 对应的 long 值；若入参为 null 则返回 0L
     */
    public static Long toLong(BigDecimal value) {
        return value == null ? 0L : value.longValue();
    }

    /**
     * BigDecimal 安全转换为 Float
     *
     * @param value 待转换的 BigDecimal 对象
     * @return 对应的 float 值；若入参为 null 则返回 0.0f
     */
    public static Float toFloat(BigDecimal value) {
        return value == null ? 0.0f : value.floatValue();
    }

    /* ==================================== 精度保留与格式化 (Scale & Formatting) ==================================== */

    /**
     * 调整 BigDecimal 小数位数（默认采用四舍五入模式 {@link RoundingMode#HALF_UP}）
     *
     * @param value 待处理的 BigDecimal 对象
     * @param scale 需保留的小数位数
     * @return 调整精度后的 BigDecimal 对象；若入参为 null 则返回指定精度的 ZERO
     */
    public static BigDecimal scale(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale, DEFAULT_ROUNDING);
        }
        return value.setScale(scale, DEFAULT_ROUNDING);
    }

    /**
     * 调整 Float 小数位数（默认采用四舍五入模式 {@link RoundingMode#HALF_UP}）
     *
     * @param value 待处理的 Float 对象
     * @param scale 需保留的小数位数
     * @return 调整精度后的 Float 对象；若入参为 null 则返回 0.0f
     */
    public static Float scale(Float value, int scale) {
        if (value == null) {
            return 0.0f;
        }
        return toBigDecimal(value).setScale(scale, DEFAULT_ROUNDING).floatValue();
    }

    /**
     * 格式化数字为标准金额/千分位字符串（例：12,345.67）
     *
     * @param value 待格式化的 BigDecimal 对象
     * @return 格式化后的千分位字符串；若入参为 null 则返回 "0.00"
     */
    public static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(value);
    }

    /**
     * 转换为指定保留位数的百分比字符串（例：0.1234 -> "12.34%"）
     *
     * @param value 待转换的比例系数 BigDecimal 对象（如 0.12）
     * @param scale 百分比数值部分需保留的小数位数
     * @return 格式化后的百分比字符串；若入参为 null 则返回带对应比例格式的 "0.00%"
     */
    public static String toPercent(BigDecimal value, int scale) {
        if (value == null) {
            return "0.00%";
        }
        BigDecimal percentValue = value.multiply(new BigDecimal("100")).setScale(scale, DEFAULT_ROUNDING);
        return percentValue.toString() + "%";
    }

    /* ==================================== BigDecimal 安全四则运算 (Safe Math) ==================================== */

    /**
     * 空安全加法计算 (a + b)
     *
     * @param a 被加数（可为 null）
     * @param b 加数（可为 null）
     * @return 运算结果 BigDecimal（绝不为 null）
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return nullToZero(a).add(nullToZero(b));
    }

    /**
     * 空安全减法计算 (a - b)
     *
     * @param a 被减数（可为 null）
     * @param b 减数（可为 null）
     * @return 运算结果 BigDecimal（绝不为 null）
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return nullToZero(a).subtract(nullToZero(b));
    }

    /**
     * 空安全乘法计算 (a * b)
     *
     * @param a 被乘数（可为 null）
     * @param b 乘数（可为 null）
     * @return 运算结果 BigDecimal；若任意入参为 null 则返回 {@link BigDecimal#ZERO}
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return BigDecimal.ZERO;
        }
        return a.multiply(b);
    }

    /**
     * 空安全除法计算 (a / b)
     * <p>默认保留 2 位小数，并采用四舍五入模式 {@link RoundingMode#HALF_UP}。</p>
     *
     * @param a 被除数（可为 null）
     * @param b 除数（可为 null 或 0）
     * @return 运算结果 BigDecimal；若被除数为 null 或除数为 0/null，返回保留 2 位小数的 ZERO
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return divide(a, b, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * 空安全除法计算 (a / b)，支持自定义保留位数和舍入模式
     *
     * @param a            被除数（可为 null）
     * @param b            除数（可为 null 或 0）
     * @param scale        保留的小数位数
     * @param roundingMode 舍入模式 {@link RoundingMode}
     * @return 运算结果 BigDecimal；若被除数为 null 或除数为 0/null，返回指定精度的 ZERO
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b, int scale, RoundingMode roundingMode) {
        if (isNullOrZero(a) || isNullOrZero(b)) {
            return BigDecimal.ZERO.setScale(scale, roundingMode);
        }
        return a.divide(b, scale, roundingMode);
    }

    /* ==================================== 比较与数值范围 (Comparison & Range) ==================================== */

    /**
     * 比较两个 BigDecimal 数值是否相等
     * <p>忽略精度差异，使用 {@link BigDecimal#compareTo(BigDecimal)} 比较，例如 2.0 与 2.00 会被认定为相等。</p>
     *
     * @param a 比较对象 a
     * @param b 比较对象 b
     * @return true: 数值相等或引用同一对象; false: 数值不等或单侧为 null
     */
    public static boolean equals(BigDecimal a, BigDecimal b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    /**
     * 检查数值是否落在闭区间 [min, max] 范围内
     *
     * @param value 待检查的数值
     * @param min   区间最小值（包含）
     * @param max   区间最大值（包含）
     * @return true: 满足 min &lt;= value &lt;= max; false: 不在该范围或任意入参为 null
     */
    public static boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null || min == null || max == null) {
            return false;
        }
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
