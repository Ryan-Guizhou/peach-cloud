package com.peach.auth.openfeign;

import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import com.peach.common.response.Response;
import com.peach.auth.openfeign.fallback.AuthFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:00
 * @Description Auth fegin remote method
 */
@FeignClient(
    contextId = ServiceContextConstant.AUTH_SERVICE_CONTEXT,
    name = ServiceNameConstant.AUTH_SERVICE,
    path = ServicePathConstant.AUTH_PATH_SERVICE,
    fallbackFactory = AuthFeignClientFallbackFactory.class
)
public interface AuthFeignClient {

    @GetMapping("/router/{routerId}")
    Response getRouterInfo(@PathVariable("routerId") String routerId);


    @GetMapping("/role/{roleId}")
    Response getRoleInfo(@PathVariable("roleId") String roleId);
}

