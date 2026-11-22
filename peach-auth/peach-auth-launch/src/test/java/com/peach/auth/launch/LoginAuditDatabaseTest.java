package com.peach.auth.launch;

import com.peach.auth.dao.LoginLogDao;
import com.peach.auth.entity.LoginLogDO;
import com.peach.auth.qo.LoginLogQO;
import com.peach.auth.service.support.LoginLogWriter;
import com.peach.auth.service.support.LoginAuditContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实MySQL验证独立审计事务、筛选、平台未归属范围及90天保留期清理；测试记录最终删除。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@EnabledIfEnvironmentVariable(named = "PEACH_LOGIN_AUDIT_DB_SMOKE", matches = "true")
class LoginAuditDatabaseTest {
    /**
     * 只启用测试所需事务代理，不启动应用或依赖其他外部服务。
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/22 20:00
     */
    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class Transactions { }

    @Test void requiresNewPersistsAcrossOuterRollbackAndMapperScopesAreEnforced() throws Exception {
        String address = System.getenv().getOrDefault("NACOS_SERVER_ADDR", "127.0.0.1:8848");
        String namespace = System.getenv().getOrDefault("NACOS_NAMESPACE", "dba0fbb1-fe66-464e-afce-33ba45c43b2b");
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://" + address + "/nacos/v1/cs/configs?dataId=peach-datasource.yml&group=PEACH-CLOUD&tenant=" + namespace)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        Map<?, ?> config = new Yaml().load(response.body());
        var hikari = (Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) config.get("spring")).get("datasource")).get("hikari");
        var ds = new DriverManagerDataSource(hikari.get("jdbc-url").toString(), hikari.get("username").toString(), hikari.get("password").toString());
        var factory = new SqlSessionFactoryBean(); factory.setDataSource(ds);
        factory.setMapperLocations(new ClassPathResource("com/peach/auth/dao/LoginLogDao.xml"));
        var template = new SqlSessionTemplate(factory.getObject());
        var dao = template.getMapper(LoginLogDao.class);
        var tx = new DataSourceTransactionManager(ds);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(Transactions.class);
            context.registerBean(PlatformTransactionManager.class, () -> tx);
            context.registerBean(LoginLogDao.class, () -> dao);
            context.registerBean(LoginLogWriter.class);
            context.refresh();
            String tenant = UUID.randomUUID().toString().replace("-", "");
            String id = UUID.randomUUID().toString().replace("-", "");
            String unassignedId = UUID.randomUUID().toString().replace("-", "");
            try {
                LoginLogDO row = record(id, tenant, LocalDateTime.now());
                new TransactionTemplate(tx).execute(status -> {
                    context.getBean(LoginLogWriter.class).save(row);
                    status.setRollbackOnly(); return null;
                });
                assertNotNull(dao.selectById(id));
                LoginLogQO query = new LoginLogQO(); query.setTenantId(tenant); query.setStartTime(LocalDateTime.now().minusHours(1)); query.setEndTime(LocalDateTime.now().plusHours(1));
                query.setAccountHash(LoginAuditContext.hashAccount("fixture-account"));
                assertEquals(1, dao.selectByQO(query).size());
                query.setTenantId("different"); assertTrue(dao.selectByQO(query).isEmpty());
                var missing = record(unassignedId, null, LocalDateTime.now().minusDays(91));
                context.getBean(LoginLogWriter.class).save(missing);
                query.setScope("UNASSIGNED"); query.setStartTime(LocalDateTime.now().minusDays(92)); query.setEndTime(LocalDateTime.now().minusDays(90));
                assertTrue(dao.selectByQO(query).stream().anyMatch(log -> log.getLogId().equals(unassignedId)));
                // Delete within a rollback transaction so real expired records remain untouched by the test.
                new TransactionTemplate(tx).execute(status -> {
                    assertTrue(dao.deleteExpired(LocalDateTime.now().minusDays(90)) >= 1);
                    assertNull(dao.selectById(unassignedId));
                    assertNotNull(dao.selectById(id));
                    status.setRollbackOnly(); return null;
                });
            } finally {
                try (var connection = ds.getConnection(); var delete = connection.prepareStatement("DELETE FROM PEACH_LOGIN_LOG WHERE LOG_ID IN (?,?)")) {
                    delete.setString(1, id); delete.setString(2, unassignedId); delete.executeUpdate();
                }
            }
        }
    }
    private static LoginLogDO record(String id, String tenant, LocalDateTime time) {
        LoginLogDO row = new LoginLogDO(); row.setLogId(id); row.setTenantId(tenant); row.setOccurredAt(time);
        row.setEventType("LOGIN"); row.setResult("FAILED"); row.setReasonCode("BAD_CREDENTIALS");
        row.setAccount("fi***t"); row.setAccountHash(LoginAuditContext.hashAccount("fixture-account"));
        row.setIpAddress("192.0.2.1"); row.setBrowser("Other"); row.setDurationMs(1L); row.setTraceId(id);
        return row;
    }
}
