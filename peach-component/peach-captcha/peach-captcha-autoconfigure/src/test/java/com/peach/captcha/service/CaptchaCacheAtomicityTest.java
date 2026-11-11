package com.peach.captcha.service;

import com.peach.captcha.constant.CaptchaConst;
import com.peach.captcha.limit.DefaultFrequencyLimitHandler;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.impl.cache.MemoryCaptchaCacheService;
import com.peach.captcha.util.MemoryCacheUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证码缓存原子消费与频率计数测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class CaptchaCacheAtomicityTest {

    @AfterEach
    void clearCache() {
        MemoryCacheUtil.clear();
    }

    @Test
    void shouldConsumeCredentialOnlyOnceAcrossConcurrentRequests() throws Exception {
        CaptchaCacheService cache = new MemoryCaptchaCacheService();
        cache.set("captcha-test-once", "payload", 60);

        List<Future<String>> tasks = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 64; i++) {
                tasks.add(executor.submit(() -> cache.getAndDelete("captcha-test-once")));
            }
            int consumed = 0;
            for (Future<String> task : tasks) {
                if ("payload".equals(task.get())) {
                    consumed++;
                }
            }
            assertEquals(1, consumed);
        }
        assertNull(cache.get("captcha-test-once"));
    }

    @Test
    void shouldApplyGetLimitUsingAtomicCounter() {
        CaptchaCacheService cache = new MemoryCaptchaCacheService();
        Properties properties = new Properties();
        properties.setProperty(CaptchaConst.REQ_GET_MINUTE_LIMIT, "2");
        DefaultFrequencyLimitHandler handler = new DefaultFrequencyLimitHandler(properties, cache);

        CaptchaVO request = new CaptchaVO();
        request.setClientUid("captcha-limit-test");

        assertNull(handler.validateGet(request));
        assertNull(handler.validateGet(request));
        assertNotNull(handler.validateGet(request));
    }
}
