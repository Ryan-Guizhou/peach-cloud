package com.peach.setting.openfeign.fallback;

import com.peach.common.constant.ServiceContextConstant;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import com.peach.setting.openfeign.SettingFeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * Setting Feign 降级工厂。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:00
 * @Description Setting Feign 降级工厂。
 */
public class SettingFeignClientFallbackFactory implements FallbackFactory<SettingFeignClient> {

    private final PeachFeignFallbackSupport fallbackSupport;

    public SettingFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        this.fallbackSupport = fallbackSupport;
    }

    @Override
    public SettingFeignClient create(Throwable cause) {
        fallbackSupport.fail(ServiceContextConstant.SETTING_SERVICE_CONTEXT, "create", cause);
        return new FallbackSettingFeignClient();
    }

    /**
     * Fallback设置Feign客户端。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private static final class FallbackSettingFeignClient implements SettingFeignClient {
    }
}
