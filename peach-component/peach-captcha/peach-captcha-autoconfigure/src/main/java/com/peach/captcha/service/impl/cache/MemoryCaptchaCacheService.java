package com.peach.captcha.service.impl.cache;

import com.peach.captcha.service.CaptchaCacheService;
import com.peach.captcha.util.MemoryCacheUtil;


/**
 * Memory验证码缓存服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:55
 */
public class MemoryCaptchaCacheService implements CaptchaCacheService {

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        MemoryCacheUtil.set(key, value, expiresInSeconds);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(MemoryCacheUtil.exists(key));
    }

    @Override
    public void delete(String key) {
        MemoryCacheUtil.remove(key);
    }

    @Override
    public String get(String key) {
        return MemoryCacheUtil.get(key);
    }
}
