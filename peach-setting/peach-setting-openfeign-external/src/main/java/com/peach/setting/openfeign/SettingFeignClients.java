package com.peach.setting.openfeign;

import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 15:01
 */
@FeignClient(
        name = ServiceNameConstant.FILE_SERVICE,
        path = ServicePathConstant.FILE_PATH_SERVICE
)
public interface SettingFeignClients {

}
