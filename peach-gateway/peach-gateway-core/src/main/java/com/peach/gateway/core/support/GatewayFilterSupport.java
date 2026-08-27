package com.peach.gateway.core.support;

import com.peach.gateway.core.constant.GatewayConstant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关过滤器通用辅助工具。
 *
 * <p>该工具只处理请求头、远端地址和安全错误响应，不读取 query、body、token 等敏感数据，
 * 用于避免网关日志和错误响应泄露私密信息。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
public final class GatewayFilterSupport {

    private GatewayFilterSupport() {
    }

    /**
     * 获取客户端地址。
     *
     * <p>优先从 {@code X-Forwarded-For} 取第一个地址；没有代理头时回退到直连远端地址。</p>
     *
     * @param exchange 当前网关交换上下文
     * @return 客户端地址；无法解析时返回 {@code unknown}
     */
    public static String clientAddress(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddress(exchange);
    }

    /**
     * 获取网关直连远端地址。
     *
     * @param exchange 当前网关交换上下文
     * @return 直连远端地址；无法解析时返回 {@code unknown}
     */
    public static String remoteAddress(ServerWebExchange exchange) {
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    /**
     * 写入网关统一 JSON 错误响应。
     *
     * @param exchange 当前网关交换上下文
     * @param status HTTP 状态码
     * @param message 可返回给客户端的安全错误信息
     * @return 响应写入完成信号
     */
    public static Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String requestId = exchange.getRequest().getHeaders().getFirst(GatewayConstant.REQUEST_ID_HEADER);
        byte[] body = ("{\"code\":" + status.value()
                + ",\"msg\":\"" + escapeJson(message)
                + "\",\"requestId\":\"" + escapeJson(requestId) + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(body)));
    }

    /**
     * 转义网关错误响应中的 JSON 字符串值。
     *
     * @param value 原始字符串
     * @return 不包含外层引号的 JSON 安全字符串
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                    break;
            }
        }
        return builder.toString();
    }
}
