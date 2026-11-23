package com.peach.code.quickstart.generator;

import com.peach.code.CodeGenerator;
import com.peach.code.CodeGeneratorException;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 内存版 {@link CodeGenerator}：在无 MySQL/Redis 时复现公开契约——租户+类型隔离、顺序递增、
 * 规则默认前缀补零格式，以及租户/类型字符校验。
 *
 * <p>不替代默认 {@code PeachCodeGenerator}；生产必须使用 MySQL 单一发号权威的自动配置实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
public class InMemoryCodeGenerator implements CodeGenerator {

    private static final Pattern SAFE_PART = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,31}");

    private static final int DEFAULT_WIDTH = 5;

    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    /**
     * 为指定租户和类型分配下一个业务编码。
     *
     * @param tenantId 租户标识，只允许字母、数字和连字符
     * @param codeType 稳定业务类型，只允许字母、数字和连字符
     * @return 格式为“前缀补零数字”的编码，例如 {@code M00001}
     * @throws CodeGeneratorException 租户或类型非法、类型未配置或序号溢出时抛出
     */
    @Override
    public String next(String tenantId, String codeType) {
        validatePart(tenantId, "tenantId");
        validatePart(codeType, "codeType");
        String type = codeType.toUpperCase(Locale.ROOT);
        String prefix = switch (type) {
            case "MENU" -> "M";
            case "ROLE" -> "R";
            case "RESOURCE" -> "RES";
            case "FUNCTION" -> "F";
            default -> throw new CodeGeneratorException("Unknown codeType in quickstart");
        };
        String key = tenantId + ":" + type;
        long value = sequences.computeIfAbsent(key, ignored -> new AtomicLong(0)).incrementAndGet();
        if (value > 99999) {
            throw new CodeGeneratorException("Code sequence exceeds MAX_CODE_WIDTH");
        }
        return format(prefix, value, DEFAULT_WIDTH);
    }

    /**
     * 清空全部内存序号，供测试隔离。
     */
    public void reset() {
        sequences.clear();
    }

    private static String format(String prefix, long value, int width) {
        String number = Long.toString(value);
        StringBuilder result = new StringBuilder(width);
        for (int i = number.length(); i < width; i++) {
            result.append('0');
        }
        result.append(number);
        return prefix + result;
    }

    private static void validatePart(String value, String name) {
        if (value == null || !SAFE_PART.matcher(value).matches()) {
            throw new CodeGeneratorException(name + " contains unsupported characters");
        }
    }
}
