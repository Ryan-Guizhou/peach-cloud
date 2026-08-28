package com.peach.monitor.openfeign.fallback;

import com.peach.common.constant.ServiceContextConstant;
import com.peach.monitor.openfeign.MonitorFeignClient;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * Monitor服务feign自动装配。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 * @Description Monitor服务feign自动装配
 */
public class MonitorFeignClientFallbackFactory implements FallbackFactory<MonitorFeignClient> {

    private final PeachFeignFallbackSupport fallbackSupport;

    public MonitorFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        this.fallbackSupport = fallbackSupport;
    }

    @Override
    public MonitorFeignClient create(Throwable cause) {
        fallbackSupport.fail(ServiceContextConstant.MONITOR_SERVICE_CONTEXT, "create", cause);
        return new FallbackMonitorFeignClient();
    }

    /**
     * Fallback监控Feign客户端。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private static final class FallbackMonitorFeignClient implements MonitorFeignClient {
    }
}
