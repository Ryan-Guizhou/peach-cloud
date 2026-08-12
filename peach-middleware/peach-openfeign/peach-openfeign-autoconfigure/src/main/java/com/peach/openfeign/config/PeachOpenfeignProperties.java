package com.peach.openfeign.config;

import com.peach.openfeign.constant.PeachOpenfeignConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Peach OpenFeign 配置属性。
 *
 * <p>该配置用于管理服务间 Feign 调用所需的 Same-Token、RequestId、超时、重试、
 * Sentinel 治理、异常响应和 fallback 校验能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
@Data
@ConfigurationProperties(prefix = "peach.openfeign")
public class PeachOpenfeignProperties {

    /**
     * 是否启用 Peach OpenFeign 扩展。
     */
    private boolean enabled = true;

    /**
     * 是否向 Feign 请求注入 Same-Token。
     */
    private boolean sameTokenEnabled = true;

    /**
     * Same-Token 缺失时是否拒绝发起 Feign 调用。
     */
    private boolean sameTokenFailFast = false;

    /**
     * 是否向 Feign 请求透传 RequestId。
     */
    private boolean requestIdEnabled = true;

    private ClientOptions client = new ClientOptions();

    /**
     * 经 Feign 调用上传时的内容长度上限（字节）。
     */
    private long uploadMaxBytes = 10L * 1024L * 1024L;

    /**
     * Feign 重试配置。
     */
    private RetryOptions retry = new RetryOptions();

    /**
     * Sentinel 治理配置。
     */
    private SentinelOptions sentinel = new SentinelOptions();

    /**
     * Feign 异常响应配置。
     */
    private ExceptionOptions exception = new ExceptionOptions();

    /**
     * Feign 降级治理配置。
     */
    private FallbackOptions fallback = new FallbackOptions();

    @Data
    public static class ClientOptions {

        private boolean okhttpEnabled = true;

        private int connectTimeoutMillis = 3_000;

        private int readTimeoutMillis = 10_000;

        private boolean followRedirects = true;

        private Map<String, NamedClientOptions> named = new LinkedHashMap<String, NamedClientOptions>(
                buildDefaultNamedClients()
        );

        private static Map<String, NamedClientOptions> buildDefaultNamedClients() {
            Map<String, NamedClientOptions> named = new LinkedHashMap<String, NamedClientOptions>();
            NamedClientOptions fileClient = new NamedClientOptions();
            fileClient.setReadTimeoutMillis(60_000);
            named.put("fileFeignClient", fileClient);
            return named;
        }
    }

    @Data
    public static class NamedClientOptions {

        private Integer connectTimeoutMillis;

        private Integer readTimeoutMillis;
    }

    @Data
    public static class RetryOptions {

        private boolean enabled = true;

        private int maxAttempts = 2;

        private long initialIntervalMillis = 100L;

        private long maxIntervalMillis = 300L;

        private double multiplier = 2.0d;

        private List<String> methods = new ArrayList<String>(PeachOpenfeignConstants.DEFAULT_RETRY_READ_METHODS);

        private List<Integer> statuses = new ArrayList<Integer>(PeachOpenfeignConstants.DEFAULT_RETRYABLE_STATUSES);

        private List<String> exceptions = new ArrayList<String>(PeachOpenfeignConstants.DEFAULT_RETRYABLE_EXCEPTIONS);
    }

    @Data
    public static class SentinelOptions {

        private boolean enabled = true;

        private String datasourceType = "nacos";

        private String flowDataId = "peach-openfeign-sentinel-flow-rules";

        private String degradeDataId = "peach-openfeign-sentinel-degrade-rules";

        private String groupId = "DEFAULT_GROUP";
    }

    @Data
    public static class ExceptionOptions {

        private boolean exposeRemoteMessage = false;

        private boolean logStacktraceFor4xx = false;

        private boolean logStacktraceFor5xx = true;
    }

    @Data
    public static class FallbackOptions {

        private boolean validateOnStartup = true;

        private boolean failFastIfMissing = true;

        private List<String> productionProfiles = new ArrayList<String>(PeachOpenfeignConstants.DEFAULT_PRODUCTION_PROFILES);
    }
}
