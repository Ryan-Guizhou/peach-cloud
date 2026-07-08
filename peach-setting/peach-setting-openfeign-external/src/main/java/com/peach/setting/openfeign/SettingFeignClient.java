package com.peach.setting.openfeign;

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
   contextId = "settingFeginClient",
   name = ServiceNameConstant.SETTING_SERVICE,
   path = ServicePathConstant.SETTING_PATH_SERVICE
)
public interface SettingFeignClient {

}
