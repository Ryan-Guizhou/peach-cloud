package com.peach.redis.tool.quickstart;

import com.peach.redis.tool.RedisDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实拓扑验证 RedisDao 跨主节点 SCAN 与跨槽逐键删除。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_TOPOLOGY_IT", matches = "true")
@SpringBootTest(properties = {
        "peach.redis.mode=${REDIS_TOPOLOGY_MODE}",
        "peach.redis.host=${REDIS_TOPOLOGY_HOSTS}",
        "peach.redis.sentinelMaster=${REDIS_TOPOLOGY_SENTINEL_MASTER:master}",
        "peach.redis.database=0",
        "peach.redis.username=${REDIS_TOPOLOGY_USERNAME:}",
        "peach.redis.password=${REDIS_TOPOLOGY_PASSWORD:}",
        "peach.redis.ssl-enabled=${REDIS_TOPOLOGY_TLS:false}",
        "quickstart.redis-tool.demo.enabled=false"
})
class RedisToolTopologyServerIntegrationTest {

    @Autowired
    private RedisDao dao;

    @Autowired
    private StringRedisTemplate strings;

    @Autowired
    private org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory;

    @Test
    void allOperationsAndCleanupModesShouldWorkAcrossSlots() throws Exception {
        RedisToolScenarioAssertions.verify(dao, connectionFactory);
    }

    @Test
    void scansAndDeletesAcrossHashSlotsWithoutAffectingUnrelatedKeys() {
        String prefix = "topology:tool:" + UUID.randomUUID() + ":";
        String first = prefix + "{alpha}:one";
        String second = prefix + "{beta}:two";
        String unrelated = "topology:unrelated:" + UUID.randomUUID();
        try {
            assertThat(dao.vSet(first, "one", 40L)).isTrue();
            assertThat(dao.vSet(second, "two", 40L)).isTrue();
            strings.opsForValue().set(unrelated, "keep");

            assertThat(dao.vGet(first)).isEqualTo("one");
            assertThat(dao.scan(prefix + "*", 20)).containsExactlyInAnyOrder(first, second);
            assertThat(dao.deletePattern(prefix + "*", 20)).isEqualTo(2L);
            assertThat(strings.opsForValue().get(unrelated)).isEqualTo("keep");
        } finally {
            strings.delete(first);
            strings.delete(second);
            strings.delete(unrelated);
        }
    }
}
