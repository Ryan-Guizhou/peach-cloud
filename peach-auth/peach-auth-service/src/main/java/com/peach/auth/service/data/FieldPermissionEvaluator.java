package com.peach.auth.service.data;

import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.ScopedFieldRuleVO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 同一有效授权证据中的字段读写权限计算和服务端脱敏。
 *
 * <p>不存在字段规则时默认 DENY；读取冲突按 DENY > MASK > ALLOW。
 * 写入只允许全部待修改字段明确 ALLOW，任一 DENY 或未知字段均拒绝。
 * 输出仅保留通过字段，不允许客户端指定脱敏策略。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
public final class FieldPermissionEvaluator {

    private static final String ALLOW = "ALLOW";
    private static final String MASK = "MASK";
    private static final String DENY = "DENY";
    private static final String PHONE = "PHONE";
    private static final String EMAIL = "EMAIL";
    private static final String FULL = "FULL";

    private FieldPermissionEvaluator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 按受控字段目录及当前对象有效授权规则生成安全输出。
     *
     * @param source 数据库对象的服务端字段映射，不应含未注册临时字段
     * @param matchingGrants 经同绑定行级授权后可访问该对象的证据
     * @param fieldRules 关联单条绑定的数据库字段策略
     * @return 不包含 DENY/未注册字段的脱敏结果
     */
    public static Map<String, Object> projectRead(Map<String, ?> source,
                                                  List<RoleDataGrantVO> matchingGrants,
                                                  List<ScopedFieldRuleVO> fieldRules) {
        if (source == null || source.isEmpty() || CollectionUtils.isEmpty(matchingGrants)) {
            return Map.of();
        }
        Map<String, Object> output = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            if (!StringUtils.hasText(entry.getKey())) {
                continue;
            }
            Decision result = decision(entry.getKey(), matchingGrants, fieldRules);
            if (DENY.equals(result.readEffect())) {
                continue;
            }
            if (MASK.equals(result.readEffect())) {
                output.put(entry.getKey(), mask(entry.getValue(), result.maskStrategy()));
            } else if (ALLOW.equals(result.readEffect())) {
                output.put(entry.getKey(), entry.getValue());
            }
        }
        return output;
    }

    /**
     * 写入前校验整个 PATCH 中的每一个字段；部分合法不允许部分提交。
     *
     * @param requested 更新的完整字段映射
     * @param matchingGrants 当前对象的授权证据
     * @param fieldRules 数据库配置的字段策略
     * @throws SecurityException 任意字段不具有明确的写权限
     */
    public static void requireWrite(Map<String, ?> requested,
                                    List<RoleDataGrantVO> matchingGrants,
                                    List<ScopedFieldRuleVO> fieldRules) {
        if (requested == null || requested.isEmpty() || CollectionUtils.isEmpty(matchingGrants)) {
            throw new SecurityException("No authorized fields were requested for update");
        }
        for (String field : requested.keySet()) {
            if (!ALLOW.equals(decision(field, matchingGrants, fieldRules).writeEffect())) {
                throw new SecurityException("Requested field update is not authorized");
            }
        }
    }

    private static Decision decision(String field, List<RoleDataGrantVO> grants,
                                     List<ScopedFieldRuleVO> rules) {
        if (!StringUtils.hasText(field) || CollectionUtils.isEmpty(rules)) {
            return new Decision(DENY, DENY, FULL);
        }
        Set<String> approvedBindings = new HashSet<>();
        for (RoleDataGrantVO grant : grants) {
            approvedBindings.add(grant.getBindingRefId());
        }
        boolean readAllow = false;
        boolean readMask = false;
        boolean readDeny = false;
        boolean writeAllow = false;
        boolean writeDeny = false;
        String maskStrategy = null;
        for (ScopedFieldRuleVO rule : rules) {
            if (rule == null || !field.equals(rule.getFieldCode())
                    || !approvedBindings.contains(rule.getBindingRefId())) {
                continue;
            }
            switch (rule.getReadEffect() == null ? DENY : rule.getReadEffect()) {
                case ALLOW -> readAllow = true;
                case MASK -> {
                    readMask = true;
                    maskStrategy = maskStrategy == null ? rule.getMaskStrategy()
                            : (maskStrategy.equals(rule.getMaskStrategy()) ? maskStrategy : FULL);
                }
                default -> readDeny = true;
            }
            if (ALLOW.equals(rule.getWriteEffect())) {
                writeAllow = true;
            } else {
                writeDeny = true;
            }
        }
        String read = readDeny ? DENY : readMask ? MASK : readAllow ? ALLOW : DENY;
        String write = writeDeny ? DENY : writeAllow ? ALLOW : DENY;
        return new Decision(read, write, maskStrategy == null ? FULL : maskStrategy);
    }

    private static Object mask(Object original, String strategy) {
        if (original == null) {
            return null;
        }
        String text = String.valueOf(original);
        if (text.isEmpty()) {
            return "";
        }
        if (PHONE.equals(strategy) && text.length() >= 7) {
            return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
        }
        if (EMAIL.equals(strategy)) {
            int at = text.indexOf('@');
            if (at > 0 && at < text.length() - 1) {
                return text.substring(0, 1) + "***" + text.substring(at);
            }
        }
        return "****";
    }

    private record Decision(String readEffect, String writeEffect, String maskStrategy) {
    }
}
