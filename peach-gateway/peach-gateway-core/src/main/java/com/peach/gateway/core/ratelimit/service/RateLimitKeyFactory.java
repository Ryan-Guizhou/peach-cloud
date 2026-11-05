package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.constant.GatewayRateLimitConstants;
import com.peach.gateway.core.ratelimit.model.MatchedRateLimitRule;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 生成有界长度、不可逆的限流 Redis Key。
  *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
*/
@Component
public class RateLimitKeyFactory {

    public String create(MatchedRateLimitRule matched, String clientIp, String tenantId,
                         String method, String path) {
        var rule = matched.rule();
        String identity = switch (rule.scope()) {
            case GLOBAL -> "global";
            case IP -> "ip:" + hash(clientIp);
            case URI -> "uri:" + hash(rule.id() + ':' + method + ':' + matched.matchedPathPattern());
            case IP_URI -> "ip-uri:" + hash(clientIp + ':' + method + ':' + matched.matchedPathPattern());
            case TENANT -> "tenant:" + hash(tenantId);
            case TENANT_URI -> "tenant-uri:" + hash(tenantId + ':' + method + ':' + matched.matchedPathPattern());
        };
        return GatewayRateLimitConstants.REDIS_KEY_PREFIX + rule.id() + ':' + identity;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
