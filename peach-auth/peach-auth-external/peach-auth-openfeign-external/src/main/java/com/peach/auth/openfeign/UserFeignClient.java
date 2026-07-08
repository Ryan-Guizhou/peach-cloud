package com.peach.auth.openfeign;

import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import com.peach.common.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    contextId = "userFeignClient",
    name = ServiceNameConstant.AUTH_SERVICE,
    path = ServicePathConstant.AUTH_PATH_SERVICE
)
public interface UserFeignClient {

    @GetMapping("/router/{routerId}")
    Response getRouterInfo(@PathVariable("routerId") String routerId);


    @GetMapping("/role/{roleId}")
    Response getRoleInfo(@PathVariable("roleId") String roleId);
}

