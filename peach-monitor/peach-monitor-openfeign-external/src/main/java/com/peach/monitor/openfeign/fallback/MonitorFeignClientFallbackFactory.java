package com.peach.monitor.openfeign.fallback;

import com.peach.common.constant.ServiceContextConstant;
import com.peach.monitor.openfeign.MonitorFeignClient;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * * @CreateTime 2025-11-25 17:47
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
        return new MonitorFeignClient() {
        };
    }
}
