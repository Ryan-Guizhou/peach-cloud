package com.peach.gateway.core.filter;

import cn.dev33.satoken.same.SaSameUtil;
import com.peach.gateway.core.constant.GatewayConstant;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关请求Global过滤器。
 * <p>网关先移除客户端试图伪造的内部 Same-Token 和最终用户转交头，
 * 再统一生成可信请求 ID；下游服务凭证只能由后续网关受信过滤器生成。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Indexed
@Component
public class GatewayRequestGlobalFilter implements GlobalFilter, Ordered {

    /** 只允许网关内部调用 Auth 时自行写入的最终用户 Token 专用头。 */
    private static final String SUBJECT_TOKEN_HEADER = "X-Peach-Subject-Token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = createRequestId();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    // 客户端永远不能向下游注入服务 Same-Token 或内部授权主体头。
                    // GatewaySameTokenGlobalFilter 仅在通过安全校验后重新签发服务凭证。
                    headers.remove(SaSameUtil.SAME_TOKEN);
                    headers.remove(SUBJECT_TOKEN_HEADER);
                    headers.remove(GatewayConstant.REQUEST_ID_HEADER);
                    headers.add(GatewayConstant.REQUEST_ID_HEADER, requestId);
                }).build();
        exchange.getResponse().getHeaders().set(GatewayConstant.REQUEST_ID_HEADER, requestId);
        return chain.filter(exchange.mutate().request(request).build());
    }


    @Override
    public int getOrder() {
        return -300;
    }

    /**
     * 创建不带连字符的请求 ID。
     *
     * @return 请求 ID
     */
    private String createRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
