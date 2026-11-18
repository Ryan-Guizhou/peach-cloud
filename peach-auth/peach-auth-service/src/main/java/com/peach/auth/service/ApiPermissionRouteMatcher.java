package com.peach.auth.service;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 服务端 API 权限目录精确路径及单路径段变量解析器。
 *
 * <p>只允许 METHOD:/literal 和 METHOD:/resource/{id} 两类目录定义。
 * 不支持 **、*、正则变量、编码斜杠、路径遍历及前缀部分匹配。
 * 更精确的已注册 API 即使未给当前成员授权，也必须盖过宽泛模板。
 * 同等精确度的多个模板命中则拒绝，不能凭数据库返回顺序放行。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 13:35
 */
public final class ApiPermissionRouteMatcher {

    public static final int MAX_CATALOG_SIZE = 512;

    private static final Pattern METHOD = Pattern.compile("GET|POST|PUT|PATCH|DELETE|HEAD");
    private static final Pattern LITERAL = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern VARIABLE = Pattern.compile("\\{[A-Za-z][A-Za-z0-9_]*\\}");

    private ApiPermissionRouteMatcher() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 检验并规范化网关构造的具体请求动作码。
     *
     * @param permissionCode 实际请求的 METHOD:/concrete/path
     * @return 规范化后的具体动作码；非法时 null
     */
    public static String normalizeRequestCode(String permissionCode) {
        if (!StringUtils.hasText(permissionCode) || permissionCode.length() > 128) {
            return null;
        }
        int separator = permissionCode.indexOf(':');
        if (separator <= 0 || separator == permissionCode.length() - 1) {
            return null;
        }
        String method = permissionCode.substring(0, separator).toUpperCase(Locale.ROOT);
        String path = permissionCode.substring(separator + 1);
        if (!METHOD.matcher(method).matches() || !validPath(path, false)) {
            return null;
        }
        return method + ":" + path;
    }

    /**
     * 在完整的当前租户/应用 API 资源目录中选择唯一最具体匹配项。
     *
     * <p>必须先确定目录里的最具体项，之后才能校验成员是否拥有该资源；
     * 不能先按成员角色过滤目录，否则无权访问的精确路由会被有权的模板覆盖。
     * 超过上限（含 SQL LIMIT 哨兵）必须拒绝，避免目录截断造成越权。</p>
     *
     * @param requestCode 已规范化的具体请求动作
     * @param catalogCodes 数据库中当前租户/应用可见的所有 API 资源码
     * @return 唯一匹配的目录权限码，无法安全解析则 null
     */
    public static String resolve(String requestCode, List<String> catalogCodes) {
        if (normalizeRequestCode(requestCode) == null
                || CollectionUtils.isEmpty(catalogCodes)
                || catalogCodes.size() > MAX_CATALOG_SIZE) {
            return null;
        }
        int colon = requestCode.indexOf(':');
        String method = requestCode.substring(0, colon);
        String path = requestCode.substring(colon + 1);
        String[] requestSegments = segments(path);
        int score = -1;
        String result = null;
        boolean ambiguous = false;
        Set<String> unique = new HashSet<>();
        for (String code : catalogCodes) {
            if (code == null || !unique.add(code) || !code.startsWith(method + ":")) {
                continue;
            }
            String pattern = code.substring(colon + 1);
            if (!validPath(pattern, true)) {
                continue;
            }
            String[] patternSegments = segments(pattern);
            if (patternSegments.length != requestSegments.length) {
                continue;
            }
            boolean matched = true;
            int staticSegments = 0;
            for (int i = 0; i < patternSegments.length; i++) {
                String segment = patternSegments[i];
                if (VARIABLE.matcher(segment).matches()) {
                    continue;
                }
                if (!segment.equals(requestSegments[i])) {
                    matched = false;
                    break;
                }
                staticSegments++;
            }
            if (!matched) {
                continue;
            }
            if (staticSegments > score) {
                score = staticSegments;
                result = code;
                ambiguous = false;
            } else if (staticSegments == score && !code.equals(result)) {
                ambiguous = true;
            }
        }
        return ambiguous ? null : result;
    }

    private static boolean validPath(String path, boolean allowVariables) {
        if (path == null || path.length() < 2 || path.length() > 128 || !path.startsWith("/")
                || path.endsWith("/") || path.contains("//")) {
            return false;
        }
        for (String segment : segments(path)) {
            if (".".equals(segment) || "..".equals(segment)) {
                return false;
            }
            if (!LITERAL.matcher(segment).matches()
                    && !(allowVariables && VARIABLE.matcher(segment).matches())) {
                return false;
            }
        }
        return true;
    }

    private static String[] segments(String path) {
        return path.substring(1).split("/", -1);
    }
}
