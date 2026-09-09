package com.peach.common.util.desensitize;

import com.peach.common.util.StringUtil;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * 敏感字段脱敏工具，默认输出掩码而非明文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:20
 */
public final class DesensitizeUtil {

    private static final char DEFAULT_MASK = '*';

    private static final String SECRET_PREFIX = "****";

    private static final String LOG_CONTENT_PREFIX = "[CONTENT_LENGTH:";

    private static final String SHORT_MASK = "***";

    private static final int DEFAULT_ERROR_MESSAGE_MAX_LENGTH = 1000;

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\D");

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    private static final Pattern SENSITIVE_VALUE_PATTERN = Pattern.compile(
            "(?i)(password|passwd|token|secret|access[_-]?key|secret[_-]?key|credential)(\\s*[:=]\\s*)[^,;\\s}\\]]+");

    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[^,;\\s]+");

    private DesensitizeUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 按类型脱敏。
     *
     * @param value 原始值
     * @param type  脱敏类型
     * @return 脱敏结果；{@code null} 输入返回 {@code null}，空白输入返回原值
     */
    public static String mask(String value, DesensitizeType type) {
        if (value == null) {
            return null;
        }
        if (StringUtil.isBlank(value)) {
            return value;
        }
        if (StringUtil.isBlank(type)) {
            throw new IllegalArgumentException("desensitize type must not be null");
        }
        return switch (type) {
            case MOBILE -> maskMobile(value);
            case EMAIL -> maskEmail(value);
            case ID_CARD -> maskIdCard(value);
            case BANK_CARD -> maskBankCard(value);
            case NAME -> maskName(value);
            case SECRET -> maskSecret(value);
            case ADDRESS -> maskAddress(value);
            case IP -> maskIp(value);
            case GENERAL -> maskMiddle(value, 3, 3);
        };
    }

    /**
     * 手机号脱敏，保留前 3 后 4。
     */
    public static String maskMobile(String mobile) {
        if (StringUtil.isBlank(mobile)) {
            return mobile;
        }
        String trimmed = mobile.trim();
        if (trimmed.startsWith("+86")) {
            String digits = digitsOnly(trimmed.substring(3));
            return "+86" + maskMiddle(digits, 3, 4);
        }
        return maskMiddle(digitsOnly(trimmed), 3, 4);
    }

    /**
     * 邮箱脱敏，保留本地部分前 2 位及完整域名。
     */
    public static String maskEmail(String email) {
        if (StringUtil.isBlank(email)) {
            return email;
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return maskMiddle(trimmed, 1, 0);
        }
        String localPart = trimmed.substring(0, atIndex);
        String domainPart = trimmed.substring(atIndex);
        int prefixKeep = Math.min(2, localPart.length());
        return maskMiddle(localPart, prefixKeep, 0) + domainPart;
    }

    /**
     * 身份证号脱敏，保留前 6 后 4。
     */
    public static String maskIdCard(String idCard) {
        if (StringUtil.isBlank(idCard)) {
            return idCard;
        }
        return maskMiddle(idCard.trim(), 6, 4);
    }

    /**
     * 银行卡号脱敏，保留前 4 后 4。
     */
    public static String maskBankCard(String bankCard) {
        if (StringUtil.isBlank(bankCard)) {
            return bankCard;
        }
        return maskMiddle(digitsOnly(bankCard), 4, 4);
    }

    /**
     * 姓名脱敏，保留首尾字符。
     */
    public static String maskName(String name) {
        if (StringUtil.isBlank(name)) {
            return name;
        }
        String trimmed = name.trim();
        int length = trimmed.length();
        if (length == 1) {
            return String.valueOf(DEFAULT_MASK);
        }
        if (length == 2) {
            return trimmed.charAt(0) + String.valueOf(DEFAULT_MASK);
        }
        return maskMiddle(trimmed, 1, 1);
    }

    /**
     * 密钥/密码脱敏，保留末尾最多 4 位。
     */
    public static String maskSecret(String secret) {
        if (StringUtil.isBlank(secret)) {
            return secret;
        }
        String trimmed = secret.trim();
        int visible = Math.min(4, trimmed.length());
        return SECRET_PREFIX + trimmed.substring(trimmed.length() - visible);
    }

    /**
     * 地址脱敏，保留前 6 个字符。
     */
    public static String maskAddress(String address) {
        if (StringUtil.isBlank(address)) {
            return address;
        }
        return maskMiddle(address.trim(), 6, 0);
    }

    /**
     * IPv4 脱敏，保留前两段。
     */
    public static String maskIp(String ip) {
        if (StringUtil.isBlank(ip)) {
            return ip;
        }
        String trimmed = ip.trim();
        if (!IPV4_PATTERN.matcher(trimmed).matches()) {
            return maskMiddle(trimmed, 3, 0);
        }
        int firstDot = trimmed.indexOf('.');
        int secondDot = trimmed.indexOf('.', firstDot + 1);
        return trimmed.substring(0, secondDot) + ".*.*";
    }

    /**
     * 通用中间掩码。
     *
     * @param value      原始值
     * @param prefixKeep 保留前缀长度
     * @param suffixKeep 保留后缀长度
     * @return 脱敏结果
     */
    public static String maskMiddle(String value, int prefixKeep, int suffixKeep) {
        return maskMiddle(value, prefixKeep, suffixKeep, DEFAULT_MASK);
    }

    /**
     * 通用中间掩码。
     *
     * @param value      原始值
     * @param prefixKeep 保留前缀长度
     * @param suffixKeep 保留后缀长度
     * @param maskChar   掩码字符
     * @return 脱敏结果
     */
    public static String maskMiddle(String value, int prefixKeep, int suffixKeep, char maskChar) {
        if (StringUtil.isBlank(value)) {
            return value;
        }
        if (prefixKeep < 0 || suffixKeep < 0) {
            throw new IllegalArgumentException("prefixKeep and suffixKeep must not be negative");
        }
        int length = value.length();
        if (prefixKeep + suffixKeep >= length) {
            return repeat(maskChar, length);
        }
        String prefix = value.substring(0, prefixKeep);
        String suffix = value.substring(length - suffixKeep);
        int maskLength = length - prefixKeep - suffixKeep;
        return prefix + repeat(maskChar, maskLength) + suffix;
    }

    /**
     * 日志场景脱敏，仅保留内容长度，避免输出明文。
     */
    public static String maskForLog(String content) {
        if (content == null) {
            return null;
        }
        return LOG_CONTENT_PREFIX + content.length() + "]";
    }

    /**
     * 短文本脱敏，保留前 2 后 2；长度不超过 4 时返回 {@code ***}。
     */
    public static String maskPlain(String value) {
        if (StringUtil.isBlank(value)) {
            return value;
        }
        if (value.length() <= 4) {
            return SHORT_MASK;
        }
        return value.substring(0, 2) + SHORT_MASK + value.substring(value.length() - 2);
    }

    /**
     * 域名脱敏，保留最后两段标签。
     */
    public static String maskHost(String host) {
        if (StringUtil.isBlank(host)) {
            return host;
        }
        String trimmed = host.trim();
        String[] labels = trimmed.split("\\.");
        if (labels.length <= 1) {
            return maskPlain(trimmed);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                builder.append('.');
            }
            if (i < labels.length - 2) {
                builder.append(SHORT_MASK);
            } else {
                builder.append(labels[i]);
            }
        }
        return builder.toString();
    }

    /**
     * 存储路径脱敏，仅保留最后一段目录名。
     */
    public static String maskStoragePath(String path) {
        if (StringUtil.isBlank(path)) {
            return path;
        }
        String normalized = path.trim().replace('\\', '/');
        String[] parts = normalized.split("/");
        String last = null;
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i] != null && !parts[i].isBlank()) {
                last = parts[i].trim();
                break;
            }
        }
        if (last == null) {
            return SHORT_MASK;
        }
        return SHORT_MASK + "/" + last;
    }

    /**
     * Endpoint / 域名 URL 脱敏。
     */
    public static String maskEndpoint(String value) {
        if (StringUtil.isBlank(value)) {
            return value;
        }
        String trimmed = value.trim();
        try {
            URI uri = URI.create(trimmed.contains("://") ? trimmed : "https://" + trimmed);
            String host = uri.getHost();
            String scheme = uri.getScheme();
            if (host == null || host.isBlank()) {
                return maskPlain(trimmed);
            }
            return (scheme == null ? "" : scheme + "://") + maskHost(host);
        } catch (Exception ex) {
            return maskPlain(trimmed);
        }
    }

    /**
     * 错误消息脱敏：清理换行、掩盖敏感键值和 Bearer Token，并按长度截断。
     */
    public static String sanitizeErrorMessage(String message, int maxLength) {
        return sanitizeErrorMessage(message, maxLength, null);
    }

    /**
     * 错误消息脱敏：{@code message} 为空时使用 {@code fallback}。
     */
    public static String sanitizeErrorMessage(String message, int maxLength, String fallback) {
        String value = message == null ? fallback : message;
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ');
        normalized = SENSITIVE_VALUE_PATTERN.matcher(normalized).replaceAll("$1$2***");
        normalized = BEARER_PATTERN.matcher(normalized).replaceAll("Bearer ***");
        int limit = maxLength > 0 ? maxLength : DEFAULT_ERROR_MESSAGE_MAX_LENGTH;
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit);
    }

    private static String digitsOnly(String value) {
        return DIGITS_ONLY.matcher(value).replaceAll("");
    }

    private static String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        return String.valueOf(ch).repeat(count);
    }
}
