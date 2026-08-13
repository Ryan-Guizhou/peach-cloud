package com.peach.monitor.openfeign;

import org.springframework.stereotype.Indexed;
import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import com.peach.monitor.openfeign.fallback.MonitorFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 15:01
 * @Description setting 服务 Feign 客户端定义
 */
@Indexed
@FeignClient(
        contextId = ServiceContextConstant.MONITOR_SERVICE_CONTEXT,
        name = ServiceNameConstant.MONITOR_SERVICE,
        path = ServicePathConstant.MONITOR_PATH_SERVICE,
        fallbackFactory = MonitorFeignClientFallbackFactory.class
)
public interface MonitorFeignClient {

}
