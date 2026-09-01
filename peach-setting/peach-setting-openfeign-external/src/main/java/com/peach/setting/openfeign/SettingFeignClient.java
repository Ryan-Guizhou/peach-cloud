package com.peach.setting.openfeign;

import org.springframework.stereotype.Indexed;
import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import com.peach.common.response.Response;
import com.peach.setting.openfeign.fallback.SettingFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * setting 服务 Feign 客户端定义。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 15:01
 * @Description setting 服务 Feign 客户端定义
 */
@Indexed
@FeignClient(
   contextId = ServiceContextConstant.SETTING_SERVICE_CONTEXT,
   name = ServiceNameConstant.SETTING_SERVICE,
   path = ServicePathConstant.SETTING_PATH_SERVICE,
   fallbackFactory = SettingFeignClientFallbackFactory.class
)
public interface SettingFeignClient {

    @GetMapping("/valueSet/item/list/{valueSetCode}")
    Response listValueSetItems(@PathVariable("valueSetCode") String valueSetCode);
}
