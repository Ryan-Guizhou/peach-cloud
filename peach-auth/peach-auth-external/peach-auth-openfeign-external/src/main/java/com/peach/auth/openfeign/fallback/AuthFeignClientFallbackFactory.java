package com.peach.auth.openfeign.fallback;

import com.peach.auth.openfeign.AuthFeignClient;
import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.response.Response;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:00
 * @Description Auth Feign 降级工厂。
 */
public class AuthFeignClientFallbackFactory implements FallbackFactory<AuthFeignClient> {

    private final PeachFeignFallbackSupport fallbackSupport;

    public AuthFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        this.fallbackSupport = fallbackSupport;
    }

    @Override
    public AuthFeignClient create(Throwable cause) {
        return new AuthFeignClient() {
            @Override
            public Response getRouterInfo(String routerId) {
                return fallbackSupport.fail(ServiceContextConstant.AUTH_SERVICE_CONTEXT, "getRouterInfo", cause);
            }

            @Override
            public Response getRoleInfo(String roleId) {
                return fallbackSupport.fail(ServiceContextConstant.AUTH_SERVICE_CONTEXT, "getRoleInfo", cause);
            }
        };
    }
}
