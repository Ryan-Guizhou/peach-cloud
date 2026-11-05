package com.peach.gateway.core.ratelimit.config;

import com.peach.gateway.core.ratelimit.model.RateLimitAlgorithm;
import com.peach.gateway.core.ratelimit.model.RateLimitScope;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关多算法限流配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Data
@ConfigurationProperties(prefix = "peach.gateway.rate-limit")
public class GatewayRateLimitProperties {

    /**
     * 是否启用限流引擎。
     */
    private boolean enabled;

    /**
     * Redis 不可用时的处理策略。
     */
    private FailureMode failureMode = FailureMode.FAIL_CLOSED;

    /**
     * 是否要求已登录用户必须具备可信租户上下文。
     */
    private boolean requireTenantContext = true;

    /**
     * 允许提供真实客户端地址的可信代理 CIDR。
     * 未配置可信代理时，始终使用 Gateway TCP 对端地址，不信任转发头。
     */
    private List<String> trustedProxies = new ArrayList<>();

    /**
     * 限流规则。
     */
    private List<RuleProperties> rules = new ArrayList<>();

    /**
     * Lua 脚本来源与加载配置。
     */
    private ScriptProperties scripts = new ScriptProperties();

    public enum FailureMode {
        FAIL_OPEN,
        FAIL_CLOSED
    }

    @Data
    public static class RuleProperties {

        private String id;

        private boolean enabled = true;

        private RateLimitScope scope;

        private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

        /**
         * 数值越小，规则越先执行。
         */
        private int priority = 100;

        private List<String> methods = new ArrayList<>();

        private List<String> paths = new ArrayList<>();

        private List<String> routeIds = new ArrayList<>();

        /**
         * 固定窗口和滑动窗口的窗口请求数上限。
         */
        private int limit = 100;

        /**
         * 固定窗口和滑动窗口的时间窗口。
         */
        private Duration window = Duration.ofSeconds(1);

        /**
         * 令牌桶和 GCRA 的令牌/请求生成速率，单位：次/秒。
         */
        private double replenishRate = 100D;

        /**
         * 令牌桶、漏桶和 GCRA 的容量。
         */
        private int burstCapacity = 100;

        /**
         * 漏桶的泄漏速率，单位：次/秒。
         */
        private double leakRate = 100D;

        /**
         * 单次请求消耗的额度。
         */
        private int cost = 1;
    }

    @Data
    public static class ScriptProperties {

        /**
         * OSS 脚本目录地址。配置后优先从该地址读取各算法脚本。
         * 目录地址应以斜杠结尾，远程地址必须是 HTTP(S) URL。
         */
        private String remoteBaseUrl;

        /**
         * 单个算法脚本的独立远程 URL，可用于 OSS 预签名 URL。
         * Key 支持 TOKEN_BUCKET 或 token-bucket 等算法名称。
         */
        private Map<String, String> remoteUrls = new LinkedHashMap<>();

        /**
         * 未配置远程地址时使用的本地 classpath 目录。
         */
        private String localLocation = "classpath:/ratelimit/lua/";

        /**
         * 远程下载失败时是否允许回退到本地脚本。
         */
        private boolean fallbackToLocal;

        /**
         * 远程脚本必须提供 SHA-256 校验值，避免未经校验的脚本被执行。
         */
        private boolean requireRemoteChecksum = true;

        private Duration connectTimeout = Duration.ofSeconds(2);

        private Duration readTimeout = Duration.ofSeconds(5);

        private int maxScriptBytes = 65536;

        /**
         * SHA-256 校验值，Key 支持算法枚举名或 kebab-case 名称。
         */
        private Map<String, String> expectedSha256 = new LinkedHashMap<>();
    }
}
