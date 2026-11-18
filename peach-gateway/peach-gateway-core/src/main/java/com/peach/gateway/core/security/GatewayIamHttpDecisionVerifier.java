package com.peach.gateway.core.security;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.peach.common.constant.ServiceNameConstant;
import com.peach.gateway.core.config.GatewayIamProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * 非阻塞访问 peach-auth 的当前 API 授权事实源。
 *
 * <p>从当前请求 Sa-Token 读取真实最终用户 Token，并从 Sa-Token 服务端生成
 * 独立 Same-Token。两种凭证只有在受信客户端与 Auth Same-Token 校验后才有效。
 * Auth 每次访问数据库核验成员和授权版本，不以旧缓存冒充最新事实；
 * 返回的成员快照与本次网关请求逐字段核对，避免切租户跨请求混用。
 * Token 和 Same-Token 不写日志、不加入 URI、不传给客户端。
 * 网络故障、超时、HTTP 非 2xx、缺少版本或上下文不一致均拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:55
 */
@Slf4j
public class GatewayIamHttpDecisionVerifier implements GatewayIamDecisionVerifier {

    private static final String SUBJECT_TOKEN_HEADER = "X-Peach-Subject-Token";
    private static final String IAM_API_PATH = "/auth/external/iam/check-api";

    private final WebClient client;
    private final Duration timeout;

    /**
     * 创建受信 IAM WebClient；Builder 必须是 @LoadBalanced Bean。
     *
     * @param builder Spring Cloud LoadBalancer 注入的 WebClient 构建器
     * @param properties IAM 请求超时和启用属性
     */
    public GatewayIamHttpDecisionVerifier(WebClient.Builder builder, GatewayIamProperties properties) {
        if (properties == null || properties.getTimeout() == null
                || properties.getTimeout().isZero() || properties.getTimeout().isNegative()) {
            throw new IllegalArgumentException("Positive gateway IAM timeout is required");
        }
        this.client = builder.baseUrl("http://" + ServiceNameConstant.AUTH_SERVICE).build();
        this.timeout = properties.getTimeout();
    }

    @Override
    public Mono<Boolean> authorize(ServerWebExchange exchange, GatewayTokenContext context,
                                   String apiResourceCode) {
        if (exchange == null || context == null || !StringUtils.hasText(apiResourceCode)) {
            return Mono.just(false);
        }
        return Mono.fromCallable(() -> {
                    SaReactorSyncHolder.setContext(exchange);
                    try {
                        String subjectToken = StpUtil.getTokenValue();
                        String sameToken = SaSameUtil.getToken();
                        if (!StringUtils.hasText(subjectToken) || !StringUtils.hasText(sameToken)) {
                            return null;
                        }
                        return new TrustedCredentials(subjectToken, sameToken);
                    } finally {
                        SaReactorSyncHolder.clearContext();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(credentials -> requestAuthorization(
                        context, apiResourceCode, credentials.subjectToken(), credentials.sameToken()))
                .timeout(timeout)
                .defaultIfEmpty(false)
                .onErrorResume(error -> {
                    log.warn("Gateway IAM remote decision unavailable, reason={}",
                            error.getClass().getSimpleName());
                    return Mono.just(false);
                });
    }

    /**
     * 向真实认证服务发送 Same-Token 和最终用户 Token 双凭证，禁止任何缓存授权兜底。
     *
     * <p>独立成方法以便用模拟 HTTP 服务测试完整协议；无凭证、非 2xx、
     * 空响应、超时及身份/版本不一致均返回拒绝。调用不阻塞 Netty EventLoop。</p>
     */
    Mono<Boolean> requestAuthorization(GatewayTokenContext context, String resourceCode,
                                       String subjectToken, String sameToken) {
        if (context == null || !StringUtils.hasText(resourceCode)
                || !StringUtils.hasText(subjectToken) || !StringUtils.hasText(sameToken)) {
            return Mono.just(false);
        }
        return client.post()
                .uri(IAM_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(SUBJECT_TOKEN_HEADER, subjectToken)
                .header(SaSameUtil.SAME_TOKEN, sameToken)
                .bodyValue(Map.of("permissionCode", resourceCode))
                .retrieve()
                .bodyToMono(IamApiDecision.class)
                .map(decision -> isMatchingDecision(context, decision))
                .timeout(timeout)
                .defaultIfEmpty(false)
                .onErrorResume(error -> {
                    log.warn("Gateway IAM HTTP decision denied, reason={}",
                            error.getClass().getSimpleName());
                    return Mono.just(false);
                });
    }

    /**
     * 仅将带有同次真实数据库成员版本证明的动作决策视为允许。
     *
     * @param snapshot 网关当前 Token 所见快照
     * @param decision Auth 请求期的授权与验证后的成员身份
     * @return 完全一致且授权有效才允许
     */
    static boolean isMatchingDecision(GatewayTokenContext snapshot, IamApiDecision decision) {
        if (snapshot == null || decision == null || !decision.allowed() || decision.context() == null) {
            return false;
        }
        IamVerifiedContext actual = decision.context();
        return Objects.equals(snapshot.userRefId(), actual.userRefId())
                && Objects.equals(snapshot.tenantRefId(), actual.tenantRefId())
                && Objects.equals(snapshot.memberRefId(), actual.memberRefId())
                && Objects.equals(snapshot.appRefId(), actual.appRefId())
                && snapshot.year() == actual.year()
                && snapshot.memberVersion() == actual.memberVersion()
                && snapshot.authVersion() == actual.authVersion();
    }

    private record TrustedCredentials(String subjectToken, String sameToken) {
    }

    /**
     * 仅用于解码 Auth 受信响应，JSON 中不包含任何登录 Token。
     */
    public record IamApiDecision(boolean allowed, IamVerifiedContext context) {
    }

    /**
     * Auth 已重新核验的最小成员身份和授权版本。
     */
    public record IamVerifiedContext(String userRefId, String tenantRefId, String memberRefId,
                                     String appRefId, int year, long memberVersion, long authVersion) {
    }
}
