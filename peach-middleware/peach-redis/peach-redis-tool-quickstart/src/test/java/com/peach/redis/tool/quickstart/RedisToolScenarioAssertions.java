package com.peach.redis.tool.quickstart;

import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.quickstart.config.RedisToolDemoContext;
import com.peach.redis.tool.quickstart.config.RedisToolDemoProperties;
import com.peach.redis.tool.quickstart.enums.RedisToolKey;
import com.peach.redis.tool.quickstart.example.RedisToolOperationsExample;
import com.peach.redis.tool.quickstart.example.SessionStringExample;
import com.peach.redis.tool.quickstart.example.UserProfileHashExample;
import com.peach.redis.tool.quickstart.example.UserTagSetExample;
import com.peach.redis.tool.quickstart.runner.RedisToolDemoRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在真实连接上执行完整演示并记录方法签名，防止新增接口遗漏示例或重载。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 16:00
 */
final class RedisToolScenarioAssertions {
    private RedisToolScenarioAssertions() {
    }

    static void verify(RedisDao delegate, RedisConnectionFactory factory) throws Exception {
        Set<Method> invoked = new HashSet<>();
        RedisDao tracked = (RedisDao) Proxy.newProxyInstance(RedisDao.class.getClassLoader(),
                new Class<?>[]{RedisDao.class}, (proxy, method, args) -> {
                    invoked.add(method);
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
        RedisToolDemoProperties properties = new RedisToolDemoProperties();
        RedisToolDemoContext context = new RedisToolDemoContext(properties, tracked);
        RedisToolOperationsExample operations = new RedisToolOperationsExample(tracked, context, factory);
        RedisToolDemoRunner runner = new RedisToolDemoRunner(new SessionStringExample(tracked, context),
                new UserProfileHashExample(tracked, context), new UserTagSetExample(tracked, context),
                operations, context, properties);
        String list = context.key(RedisToolKey.LIST, "main");
        String hash = context.key(RedisToolKey.HASH, "main");
        String missing = context.key(RedisToolKey.VALUE, "missing");
        RedisToolDemoContext other = new RedisToolDemoContext(new RedisToolDemoProperties(), delegate);
        String unrelated = other.key(RedisToolKey.VALUE, "main");
        try {
            delegate.vSet(list, "wrong-type-stale-data");
            delegate.vSet(missing, "stale-data");
            delegate.vSet(unrelated, "preserve");
            runner.run(new DefaultApplicationArguments());
            assertThat(delegate.existsKey(hash)).isTrue();
            assertThat(delegate.existsKey(list)).isTrue();
            assertThat(delegate.existsKey(missing)).isFalse();
            assertThat(delegate.vGet(unrelated)).isEqualTo("preserve");
            // 同一标识重复运行，前置清理必须让计数器、集合及错误类型恢复到干净状态。
            runner.run(new DefaultApplicationArguments());
            properties.setCleanupAfterRun(true);
            runner.run(new DefaultApplicationArguments());
            assertThat(delegate.existsKey(hash)).isFalse();
            assertThat(delegate.existsKey(list)).isFalse();
            assertThat(delegate.vGet(unrelated)).isEqualTo("preserve");
            assertThat(invoked).containsAll(Set.of(RedisDao.class.getDeclaredMethods()));
        } finally {
            context.clear();
            other.clear();
        }
    }
}
