package com.peach.auth.openfeign.fallback;

import com.peach.auth.openfeign.AuthFeignClient;
import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.response.Response;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * Auth Feign 降级工厂。
 *
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
        return new FallbackAuthFeignClient(fallbackSupport, cause);
    }

    /**
     * Fallback认证Feign客户端。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */


    private static final class FallbackAuthFeignClient implements AuthFeignClient {

        private final PeachFeignFallbackSupport fallbackSupport;
        private final Throwable cause;

        private FallbackAuthFeignClient(PeachFeignFallbackSupport fallbackSupport, Throwable cause) {
            this.fallbackSupport = fallbackSupport;
            this.cause = cause;
        }

        @Override
        public Response getRouterInfo(String routerId) {
            return fallbackSupport.fail(ServiceContextConstant.AUTH_SERVICE_CONTEXT, "getRouterInfo", cause);
        }

        @Override
        public Response getRoleInfo(String roleId) {
            return fallbackSupport.fail(ServiceContextConstant.AUTH_SERVICE_CONTEXT, "getRoleInfo", cause);
        }
    }
}
