package com.peach.userservice.openfeign;

import com.peach.common.ServiceNameConstant;
import com.peach.common.ServicePathConstant;
import com.peach.common.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = ServiceNameConstant.USER_SERVICE,
    path = ServicePathConstant.USER_PATH_SERVICE
)
public interface UserFeignClient {

    @GetMapping("/router/{routerId}")
    Response getRouterInfo(@PathVariable("routerId") String routerId);


    @GetMapping("/role/{roleId}")
    Response getRoleInfo(@PathVariable("roleId") String roleId);
}

