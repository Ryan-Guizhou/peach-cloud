package com.peach.gateway.core.ratelimit.script;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis Cluster 单 Key Lua 脚本契约测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
class RateLimitLuaScriptValidatorTest {

    private static final List<String> SCRIPT_NAMES = List.of(
            "fixed-window.lua",
            "sliding-window-log.lua",
            "sliding-window-counter.lua",
            "token-bucket.lua",
            "leaky-bucket.lua",
            "gcra.lua");

    @Test
    void shouldAcceptAllBundledRateLimitScripts() throws IOException {
        for (String scriptName : SCRIPT_NAMES) {
            String script = readScript(scriptName);
            assertThatCode(() -> RateLimitLuaScriptValidator.validate(script))
                    .as("Bundled Lua script must satisfy the single-key contract: %s", scriptName)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void shouldAcceptDirectSingleKeyCommandsAndIgnoreCommentsAndStringLiterals() {
        String script = """
                -- KEYS[2] in a comment is not executable code
                local note = "KEYS[2] is only documentation"
                local value = redis.call('GET', KEYS[1])
                redis.call('SET', KEYS[1], value or note)
                return redis.call('TIME')
                """;

        assertThatCode(() -> RateLimitLuaScriptValidator.validate(script))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectSecondRedisKey() {
        assertInvalid("return redis.call('GET', KEYS[2])");
    }

    @Test
    void shouldRejectBareKeysAlias() {
        assertInvalid("""
                local keys = KEYS
                return redis.call('GET', keys[1])
                """);
    }

    @Test
    void shouldRejectDynamicKeyIndex() {
        assertInvalid("return redis.call('GET', KEYS[tonumber(ARGV[1])])");
    }

    @Test
    void shouldRejectArgumentValueAsRedisKey() {
        assertInvalid("return redis.call('GET', ARGV[1])");
    }

    @Test
    void shouldRejectIndirectRedisClientAlias() {
        assertInvalid("""
                local client = redis
                return client.call('GET', KEYS[1])
                """);
    }

    @Test
    void shouldRejectRedisCallFunctionAlias() {
        assertInvalid("""
                local call = redis.call
                return call('GET', KEYS[1])
                """);
    }

    @Test
    void shouldRejectNonWhitelistedRedisCommand() {
        assertInvalid("return redis.call('EVAL', ARGV[1], 1, KEYS[1])");
    }

    @Test
    void shouldRejectDynamicRedisCommandName() {
        assertInvalid("return redis.call(ARGV[1], KEYS[1])");
    }

    @Test
    void shouldRejectForbiddenLuaRuntimeLibraries() {
        assertInvalid("return os.execute('id')");
        assertInvalid("return io.open('/etc/passwd', 'r')");
        assertInvalid("return loadstring(ARGV[1])");
        assertInvalid("return _G.redis.call('GET', KEYS[1])");
    }

    private void assertInvalid(String script) {
        assertThatThrownBy(() -> RateLimitLuaScriptValidator.validate(script))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String readScript(String name) throws IOException {
        String resource = "/ratelimit/lua/" + name;
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing bundled Lua script: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
