package com.peach.code.quickstart.generator;

import com.peach.code.CodeGenerator;
import com.peach.code.CodeGeneratorException;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 内存版 {@link CodeGenerator}：在无 MySQL/Redis 时复现公开契约——租户+前缀隔离、顺序递增、
 * 前缀大写补零格式，以及租户/前缀字符校验。
 *
 * <p>不替代默认 {@code PeachCodeGenerator}；生产仍应走 Redis 优先、MySQL 兜底的自动配置实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
public class InMemoryCodeGenerator implements CodeGenerator {

    private static final Pattern SAFE_PART = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,31}");

    private static final int DEFAULT_WIDTH = 8;

    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    /**
     * 为指定租户和前缀分配下一个业务编码。
     *
     * @param tenantId 租户标识，只允许字母、数字和连字符
     * @param prefix 编码前缀，只允许字母、数字和连字符
     * @return 格式为“前缀_补零数字”的编码，例如 {@code MENU_00000001}
     * @throws CodeGeneratorException 租户或前缀非法时抛出
     */
    @Override
    public String next(String tenantId, String prefix) {
        validatePart(tenantId, "tenantId");
        validatePart(prefix, "prefix");
        String key = tenantId + ":" + prefix.toUpperCase(Locale.ENGLISH);
        long value = sequences.computeIfAbsent(key, ignored -> new AtomicLong(0)).incrementAndGet();
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
        return prefix.toUpperCase(Locale.ENGLISH) + "_" + result;
    }

    private static void validatePart(String value, String name) {
        if (value == null || !SAFE_PART.matcher(value).matches()) {
            throw new CodeGeneratorException(name + " contains unsupported characters");
        }
    }
}
