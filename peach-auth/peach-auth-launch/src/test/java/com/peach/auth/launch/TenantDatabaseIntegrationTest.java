package com.peach.auth.launch;

import com.peach.auth.dao.TenantDao;
import com.peach.auth.dao.UserOrgDao;
import com.peach.auth.entity.TenantDO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.yaml.snakeyaml.Yaml;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 显式开启后验证本机已迁移数据库的租户 Mapper；测试写入全部回滚。
 * 凭据只在内存中读取，不打印或写入文件。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
@EnabledIfEnvironmentVariable(named = "PEACH_TENANT_DB_SMOKE", matches = "true")
class TenantDatabaseIntegrationTest {
    @Test
    void realCrudAndLoginQueriesShouldWorkAndRollback() throws Exception {
        String address = System.getenv().getOrDefault("NACOS_SERVER_ADDR", "127.0.0.1:8848");
        String namespace = System.getenv().getOrDefault("NACOS_NAMESPACE", "dba0fbb1-fe66-464e-afce-33ba45c43b2b");
        var uri = URI.create("http://" + address + "/nacos/v1/cs/configs?dataId=peach-datasource.yml&group=PEACH-CLOUD&tenant=" + namespace);
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        Map<?, ?> root = new Yaml().load(response.body());
        var hikari = (Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) root.get("spring")).get("datasource")).get("hikari");
        var configuration = new Configuration();
        for (String name : new String[]{"TenantDao", "UserOrgDao"}) {
            String resource = "com/peach/auth/dao/" + name + ".xml";
            try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        try (var connection = DriverManager.getConnection(hikari.get("jdbc-url").toString(),
                hikari.get("username").toString(), hikari.get("password").toString())) {
            connection.setAutoCommit(false);
            try (var session = new SqlSessionFactoryBuilder().build(configuration).openSession(connection)) {
                try {
                    var dao = session.getMapper(TenantDao.class);
                    String id = UUID.randomUUID().toString().replace("-", "");
                    var tenant = new TenantDO();
                    tenant.setId(id); tenant.setTenantId(id); tenant.setTenantCode("SMOKE_" + id);
                    tenant.setTenantName("Rollback smoke test"); tenant.setTenantType("ENTERPRISE");
                    tenant.setStatus("PENDING"); tenant.setDeleted(0); tenant.setVersion(0L);
                    tenant.setCreateTime("2026-11-21 18:00:00");
                    dao.insert(tenant);
                    var stored = dao.selectById(id);
                    assertThat(stored.getTenantCode()).isEqualTo(tenant.getTenantCode());
                    stored.setTenantName("Updated smoke test");
                    assertThat(dao.updateDetails(stored)).isEqualTo(1);
                    assertThat(dao.updateDetails(stored)).isZero();
                    stored = dao.selectById(id); stored.setStatus("ACTIVE");
                    stored.setActivatedTime("2026-11-21 18:00:01");
                    assertThat(dao.updateStatus(stored)).isEqualTo(1);
                    var query = new com.peach.auth.qo.TenantQO();
                    query.setKeyword(tenant.getTenantCode());
                    assertThat(dao.selectByQO(query)).hasSize(1);
                    try (var statement = connection.prepareStatement("SELECT U.USER_ID FROM PEACH_USER_ORG U JOIN PEACH_TENANT T ON T.TENANT_ID=U.TENANT_ID WHERE T.STATUS='ACTIVE' AND T.DELETED=0 LIMIT 1"); var rows = statement.executeQuery()) {
                        assertThat(rows.next()).isTrue();
                        assertThat(session.getMapper(UserOrgDao.class).selectByUserId(rows.getString(1))).isNotEmpty();
                    }
                    stored = dao.selectById(id); stored.setStatus("CLOSED");
                    stored.setClosedAt("2026-11-21 18:00:02");
                    assertThat(dao.updateStatus(stored)).isEqualTo(1);
                    stored = dao.selectById(id); stored.setDeleter("smoke-test");
                    stored.setDeleteTime("2026-11-21 18:00:03");
                    assertThat(dao.logicalDelete(stored)).isEqualTo(1);
                    assertThat(dao.selectById(id)).isNull();
                } finally { session.rollback(); }
            }
        }
    }
}
