package com.peach.setting.openfeign;

import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import com.peach.setting.openfeign.fallback.SettingFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 15:01
 * @Description setting 服务 Feign 客户端定义
 */
@FeignClient(
   contextId = ServiceContextConstant.SETTING_SERVICE_CONTEXT,
   name = ServiceNameConstant.SETTING_SERVICE,
   path = ServicePathConstant.SETTING_PATH_SERVICE,
   fallbackFactory = SettingFeignClientFallbackFactory.class
)
public interface SettingFeignClient {

}
