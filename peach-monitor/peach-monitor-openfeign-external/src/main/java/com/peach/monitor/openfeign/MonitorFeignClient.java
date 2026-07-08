package com.peach.monitor.openfeign;

import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 15:01
 * @Description setting 服务 Feign 客户端定义
 */
@FeignClient(
        name = ServiceNameConstant.MONITOR_SERVICE,
        path = ServicePathConstant.MONITOR_PATH_SERVICE
)
public interface MonitorFeignClient {

}
