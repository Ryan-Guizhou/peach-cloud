package com.peach.openfeign.support;

import cn.dev33.satoken.same.SaSameUtil;
import com.peach.openfeign.config.PeachOpenfeignProperties;
import com.peach.openfeign.constant.PeachOpenfeignConstants;
import com.peach.openfeign.exception.FeignSameTokenException;
import com.peach.openfeign.exception.FeignUploadSizeLimitException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Peach OpenFeign 请求拦截器。
 *
 * <p>作为 Feign 的 {@link RequestInterceptor} 实现，在每次服务间调用（通过 Feign 客户端发起 HTTP 请求）之前执行。
 * 主要职责包括：</p>
 * <ul>
 *   <li><b>请求体大小校验：</b>若配置了上传最大字节数，检查请求体是否超限，超限则快速失败，避免网络传输浪费。</li>
 *   <li><b>链路追踪标识中继：</b>将当前 HTTP 请求头中的 <code>X-Request-Id</code> 透传到下游服务，保障全链路追踪能力。</li>
 *   <li><b>服务间认证凭据注入：</b>注入 Sa-Token 的 Same-Token，用于内部服务间的身份验证，防止外部直接调用。</li>
 * </ul>
 *
 * <p><b>执行时机：</b>每当 Spring Cloud OpenFeign 的代理对象发起 HTTP 请求时，会先调用此拦截器的 {@link #apply(RequestTemplate)} 方法，
 * 对请求模板（包括 URL、头、体等）进行增强或校验，然后再由 Feign 底层执行真正的网络请求。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/8/12
 */
@Slf4j
public class PeachOpenfeignRequestInterceptor implements RequestInterceptor {

    private final PeachOpenfeignProperties properties;

    public PeachOpenfeignRequestInterceptor(PeachOpenfeignProperties properties) {
        this.properties = properties;
    }

    /**
     * 拦截并处理 Feign 请求模板。
     * <p>按顺序执行：大小校验 → 注入请求ID → 注入 Same-Token。</p>
     *
     * @param template Feign 请求模板，包含 URL、方法、头、请求体等信息
     */
    @Override
    public void apply(RequestTemplate template) {
        log.debug("[PeachFeign] intercepting request method={} path={}", template.method(), safePath(template));

        // 1. 校验请求体大小，防止超大上传
        assertUploadSize(template);

        // 2. 中继当前请求的 RequestId（若启用）
        addRequestId(template);

        // 3. 注入Authorizaion  主要是为了在任何服务都可以使用到上下文
        sanitizeAuthorization(template);

        // 4. 注入 Same-Token（若启用）
        addSameToken(template);

        log.debug("[PeachFeign] request intercepted method={} path={}", template.method(), safePath(template));
    }

    /**
     * 校验请求体大小是否超出配置的上限。
     * <p>若请求体长度超过 {@link PeachOpenfeignProperties#getUploadMaxBytes()}，则抛出 {@link FeignUploadSizeLimitException}，
     * 快速失败，避免无效的大流量消耗。</p>
     *
     * @param template Feign 请求模板
     * @throws FeignUploadSizeLimitException 当请求体大小超限时抛出
     */
    private void assertUploadSize(RequestTemplate template) {
        long maxBytes = properties.getUploadMaxBytes();
        // 未配置上限或无请求体则跳过
        if (maxBytes <= 0L || template == null || template.body() == null) {
            return;
        }

        int length = template.body().length;
        if (length > maxBytes) {
            log.warn("[PeachFeign] request body size exceeds limit contentLength={} maxBytes={} path={}",
                    length, maxBytes, safePath(template));
            throw new FeignUploadSizeLimitException(safePath(template), length, maxBytes);
        }
    }

    /**
     * 注入请求ID（Request-Id）到 Feign 请求头。
     * <p>若配置中启用 {@code requestIdEnabled}，且当前线程上下文中存在请求头（从原始HTTP请求获取），
     * 则将该头值设置到 Feign 请求模板中（若模板尚未包含该头）。</p>
     *
     * @param template Feign 请求模板
     */
    private void addRequestId(RequestTemplate template) {
        if (!properties.isRequestIdEnabled()) {
            return;
        }

        String requestId = sanitizeHeaderValue(
                PeachOpenfeignConstants.HEADER_REQUEST_ID,
                getCurrentRequestHeader(PeachOpenfeignConstants.HEADER_REQUEST_ID),
                PeachOpenfeignConstants.MAX_REQUEST_ID_LENGTH,
                template
        );
        if (requestId == null || requestId.isEmpty()) {
            log.trace("No Request-Id found in current request context, skipping relay");
            return;
        }

        // 如果模板中已存在该头，则不覆盖（避免与显式设置冲突）
        if (hasHeader(template, PeachOpenfeignConstants.HEADER_REQUEST_ID)) {
            log.trace("Request-Id header already exists in template, skip");
            return;
        }

        template.header(PeachOpenfeignConstants.HEADER_REQUEST_ID, requestId);
        log.debug("[PeachFeign] request-id relayed path={}", safePath(template));
    }

    private void sanitizeAuthorization(RequestTemplate template) {
        boolean relayAuthorization = properties.isRelayAuthorizationEnabled()
                || hasTruthyHeader(template, PeachOpenfeignConstants.HEADER_RELAY_AUTHORIZATION);
        template.removeHeader(PeachOpenfeignConstants.HEADER_RELAY_AUTHORIZATION);

        if (relayAuthorization) {
            relayAuthorization(template);
            return;
        }
        if (!hasHeader(template, PeachOpenfeignConstants.HEADER_AUTHORIZATION)) {
            return;
        }
        template.removeHeader(PeachOpenfeignConstants.HEADER_AUTHORIZATION);
        log.debug("[PeachFeign] authorization header removed for service call path={}", safePath(template));
    }

    private void relayAuthorization(RequestTemplate template) {
        if (hasHeader(template, PeachOpenfeignConstants.HEADER_AUTHORIZATION)) {
            log.debug("[PeachFeign] authorization header kept for service call path={}", safePath(template));
            return;
        }
        String authorization = sanitizeHeaderValue(
                PeachOpenfeignConstants.HEADER_AUTHORIZATION,
                getCurrentRequestHeader(PeachOpenfeignConstants.HEADER_AUTHORIZATION),
                PeachOpenfeignConstants.MAX_AUTHORIZATION_LENGTH,
                template
        );
        if (authorization == null || authorization.isEmpty()) {
            log.debug("[PeachFeign] authorization relay requested but current request has no authorization path={}",
                    safePath(template));
            return;
        }
        template.header(PeachOpenfeignConstants.HEADER_AUTHORIZATION, authorization);
        log.debug("[PeachFeign] authorization header relayed for service call path={}", safePath(template));
    }

    /**
     * 注入 Sa-Token Same-Token 到 Feign 请求头。
     * <p>Same-Token 用于服务间调用鉴权，优先从当前请求头获取（若存在则透传），否则通过 {@link SaSameUtil#getToken()} 生成新 token。</p>
     * <p>若配置 {@code sameTokenFailFast=true} 且无法获取 token，则抛出 {@link FeignSameTokenException} 快速失败；
     * 否则记录警告日志并继续（可能下游校验失败）。</p>
     *
     * @param template Feign 请求模板
     * @throws FeignSameTokenException 当需要快速失败且 token 缺失时抛出
     */
    private void addSameToken(RequestTemplate template) {
        if (!properties.isSameTokenEnabled()) {
            return;
        }

        String token = resolveSameToken();
        if (token == null || token.isEmpty()) {
            if (properties.isSameTokenFailFast()) {
                log.error("[PeachFeign] same-token missing, fail-fast enabled path={}", safePath(template));
                throw new FeignSameTokenException(safePath(template));
            }
            log.warn("[PeachFeign] same-token missing, proceeding without token path={}", safePath(template));
            return;
        }

        // 移除可能已存在的旧值，确保使用最新 token
        template.removeHeader(SaSameUtil.SAME_TOKEN);
        template.header(SaSameUtil.SAME_TOKEN, token);
        log.debug("[PeachFeign] same-token injected path={}", safePath(template));
    }

    /**
     * 解析 Same-Token，优先从当前请求头获取，否则通过 SaSameUtil 生成。
     *
     * @return token 字符串，若无法获取则返回 {@code null}
     */
    private String resolveSameToken() {
        // 1. 尝试从当前 HTTP 请求头获取（上游透传）
        String token = sanitizeHeaderValue(
                SaSameUtil.SAME_TOKEN,
                getCurrentRequestHeader(SaSameUtil.SAME_TOKEN),
                PeachOpenfeignConstants.MAX_SAME_TOKEN_LENGTH,
                null
        );
        if (token != null && !token.isEmpty()) {
            return token;
        }

        // 2. 若本地上下文中已有 Same-Token（如之前生成的），直接使用
        token = sanitizeHeaderValue(
                SaSameUtil.SAME_TOKEN,
                SaSameUtil.getToken(),
                PeachOpenfeignConstants.MAX_SAME_TOKEN_LENGTH,
                null
        );
        if (token != null && !token.isEmpty()) {
            return token;
        }

        return null;
    }

    /**
     * 从当前线程绑定的 HTTP 请求中获取指定头的值（仅当当前环境为 Web 请求时）。
     *
     * @param headerName 头名称
     * @return 头的值（已 trim），若不存在或非 Web 环境则返回 {@code null}
     */
    private String getCurrentRequestHeader(String headerName) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            // 非 Servlet 环境（如非 Web 调用），无法获取原始请求头
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        String value = request.getHeader(headerName);
        return value == null ? null : value.trim();
    }

    private String sanitizeHeaderValue(String headerName, String value, int maxLength, RequestTemplate template) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength || containsControlCharacter(trimmed)) {
            log.warn("[PeachFeign] ignored invalid outbound header header={} path={}",
                    headerName, safePath(template));
            return null;
        }
        return trimmed;
    }

    private boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 Feign 请求模板中是否已存在指定头（不区分大小写）。
     *
     * @param template   Feign 请求模板
     * @param headerName 头名称
     * @return 存在则返回 {@code true}，否则 {@code false}
     */
    private boolean hasHeader(RequestTemplate template, String headerName) {
        if (template == null || template.headers() == null || headerName == null) {
            return false;
        }
        for (String existingHeader : template.headers().keySet()) {
            if (existingHeader != null && existingHeader.equalsIgnoreCase(headerName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTruthyHeader(RequestTemplate template, String headerName) {
        if (template == null || template.headers() == null || headerName == null) {
            return false;
        }
        for (String existingHeader : template.headers().keySet()) {
            if (existingHeader == null || !existingHeader.equalsIgnoreCase(headerName)) {
                continue;
            }
            for (String value : template.headers().get(existingHeader)) {
                if ("true".equalsIgnoreCase(String.valueOf(value).trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String safePath(RequestTemplate template) {
        if (template == null || template.path() == null || template.path().trim().isEmpty()) {
            return "unknown";
        }
        return template.path();
    }
}
