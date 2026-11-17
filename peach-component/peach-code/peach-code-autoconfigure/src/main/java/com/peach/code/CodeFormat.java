package com.peach.code;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 受控业务编码格式定义与预览工具。
 *
 * <p>规则仅支持前缀、分隔符与固定位数，不执行任何自定义代码或表达式。
 * 预览不会分配序列；正式发号以 MySQL 中的当前水位与规则为准。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 15:25
 */
public record CodeFormat(String prefix, String separator, int width) {

    private static final Pattern SAFE_PREFIX = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,31}");

    /**
     * 校验编码格式。
     *
     * @param prefix 业务类型前缀
     * @param separator 分隔符，仅允许下划线或短横线
     * @param width 数字序列固定位数
     */
    public CodeFormat {
        if (prefix == null || !SAFE_PREFIX.matcher(prefix).matches()) {
            throw new CodeGeneratorException("Invalid business code prefix");
        }
        if (!"_".equals(separator) && !"-".equals(separator)) {
            throw new CodeGeneratorException("Unsupported business code separator");
        }
        if (width < 1 || width > 18) {
            throw new CodeGeneratorException("Business code width must be between 1 and 18");
        }
        prefix = prefix.toUpperCase(Locale.ENGLISH);
    }

    /**
     * 预览给定序列的最终编码，不操作数据库。
     *
     * @param sequence 已分配的序列号
     * @return 按格式渲染的业务编码
     */
    public String preview(long sequence) {
        if (sequence < 1) {
            throw new CodeGeneratorException("Business code sequence must be positive");
        }
        String number = Long.toString(sequence);
        if (number.length() > width) {
            throw new CodeGeneratorException("Business code sequence exceeds configured width");
        }
        StringBuilder builder = new StringBuilder(prefix.length() + separator.length() + width);
        builder.append(prefix).append(separator);
        for (int index = number.length(); index < width; index++) {
            builder.append('0');
        }
        return builder.append(number).toString();
    }
}
