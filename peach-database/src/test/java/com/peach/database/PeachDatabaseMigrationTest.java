package com.peach.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.DockerClientFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.yaml.snakeyaml.Yaml;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 空库、接管、重复启动、并发迁移及失败保护集成验证。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 22:00
 */
class PeachDatabaseMigrationTest {
    static MySQLContainer<?> mysql;
    static String url, username, password;
    static final List<String> schemas = new ArrayList<>();
    @BeforeAll static void database() throws Exception {
        if ("true".equals(System.getenv("PEACH_FLYWAY_DB_SMOKE"))) {
            String address=System.getenv().getOrDefault("NACOS_SERVER_ADDR","127.0.0.1:8848");
            String namespace=System.getenv().getOrDefault("NACOS_NAMESPACE","dba0fbb1-fe66-464e-afce-33ba45c43b2b");
            var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://"+address+"/nacos/v1/cs/configs?dataId=peach-datasource.yml&group=PEACH-CLOUD&tenant="+namespace)).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(200,response.statusCode());
            Map<?,?> config=new Yaml().load(response.body());
            var db=(Map<?,?>)((Map<?,?>)((Map<?,?>)config.get("spring")).get("datasource")).get("hikari");
            url=db.get("jdbc-url").toString();username=db.get("username").toString();password=db.get("password").toString();
        } else {
            boolean available=DockerClientFactory.instance().isDockerAvailable();
            if ("true".equals(System.getenv("CI"))) assertTrue(available,"Migration CI requires a working Docker daemon");
            Assumptions.assumeTrue(available,"Docker is required unless PEACH_FLYWAY_DB_SMOKE=true");
            mysql=new MySQLContainer<>("mysql:8.0.36");mysql.start();
            url=mysql.getJdbcUrl();username="root";password=mysql.getPassword();
        }
    }
    @AfterAll static void cleanup() {
        if (url!=null) {
            var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,username,password));
            for(String schema:schemas) {
                if(!schema.matches("peach_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe fixture schema");
                jdbc.execute("DROP DATABASE `"+schema+"`");
            }
        }
        if(mysql!=null) mysql.stop();
    }
    private DriverManagerDataSource source() {
        String database = "peach_" + UUID.randomUUID().toString().replace("-", "");
        var root = new DriverManagerDataSource(url,username,password);
        new JdbcTemplate(root).execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        schemas.add(database);
        String jdbcUrl=url.substring(0,url.indexOf('/',"jdbc:mysql://".length())+1)+database+(url.contains("?")?url.substring(url.indexOf('?')):"");
        return new DriverManagerDataSource(jdbcUrl,username,password);
    }
    private Flyway flyway(DriverManagerDataSource source) { return PeachDatabaseAutoConfiguration.configure(Flyway.configure().dataSource(source)).load(); }
    private void migrate(DriverManagerDataSource source) { new PeachMigrationStrategy().migrate(flyway(source)); }
    @Test void initializesCompleteFirstVersionAndKeepsRestartChanges() {
        var source=source();migrate(source);var jdbc=new JdbcTemplate(source);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY",Integer.class));
        assertEquals("1",jdbc.queryForObject("SELECT VERSION FROM PEACH_FLYWAY_HISTORY",String.class));
        assertEquals(27,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_ROUTER",Integer.class));
        assertEquals(257,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_RESOURCE",Integer.class));
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_CODE_RULE WHERE MAX_CODE_WIDTH=4",Integer.class));
        for(String spec:List.of("PEACH_MENU:MENU_CODE:M","PEACH_FUNCTION:FUNC_CODE:F","PEACH_ROUTER:ROUTER_CODE:RT","PEACH_ROLE:ROLE_CODE:RL")) {
            String[] p=spec.split(":");assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM "+p[0]+" WHERE "+p[1]+" NOT REGEXP '^"+p[2]+"[0-9]{4}$'",Integer.class));
        }
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_AUTH_RESOURCE WHERE RESOURCE_CODE='POST:/api/auth/router/pageList'",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_MENU m LEFT JOIN PEACH_FUNCTION f ON f.TENANT_ID=m.TENANT_ID AND f.APP_ID=m.APP_ID AND f.FUNC_CODE=m.FUNC_CODE WHERE m.FUNC_CODE IS NOT NULL AND f.FUNC_ID IS NULL",Integer.class));
        String tenant=jdbc.queryForObject("SELECT TENANT_ID FROM PEACH_TENANT LIMIT 1",String.class);
        var generator=new com.peach.code.autoconfigure.PeachCodeGenerator(jdbc,new com.peach.code.autoconfigure.CodeProperties(),new org.springframework.jdbc.datasource.DataSourceTransactionManager(source));
        for(String type:List.of("MENU","FUNCTION","ROUTER","ROLE")) assertTrue(generator.next(tenant,type).matches(Map.of("MENU","M","FUNCTION","F","ROUTER","RT","ROLE","RL").get(type)+"[0-9]{4}"));
        jdbc.update("UPDATE PEACH_CODE_RULE SET CURRENT_VALUE=9999 WHERE CODE_TYPE='MENU'");assertThrows(com.peach.code.CodeGeneratorException.class,()->generator.next(tenant,"MENU"));
        jdbc.update("UPDATE PEACH_ROLE SET ROLE_NAME='Custom name'");migrate(source);assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_ROLE WHERE ROLE_NAME='Custom name'",Integer.class));
    }
    @Test void refusesNonEmptyUnversionedSchemas() {
        var source=source();var jdbc=new JdbcTemplate(source);jdbc.execute("CREATE TABLE PEACH_USER(USER_ID INT)");assertThrows(Exception.class,()->migrate(source));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()",Integer.class));
    }
    @Test void rejectsLegacyHistoryRatherThanSkippingConsolidatedVersion() {
        for(String type:List.of("BASELINE","JDBC")) {
            var source=source();migrate(source);var jdbc=new JdbcTemplate(source);
            jdbc.update("UPDATE PEACH_FLYWAY_HISTORY SET VERSION='14',TYPE=?",type);
            assertThrows(Exception.class,()->migrate(source));
            assertEquals(27,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_ROUTER",Integer.class));
            assertEquals("14",jdbc.queryForObject("SELECT VERSION FROM PEACH_FLYWAY_HISTORY",String.class));
        }
    }
    @Test void blocksChecksumAndSqlFailuresWithoutCleaning() {
        var source=source();migrate(source);var jdbc=new JdbcTemplate(source);
        jdbc.update("UPDATE PEACH_FLYWAY_HISTORY SET CHECKSUM=CHECKSUM+1 WHERE VERSION='1'");assertThrows(Exception.class,()->migrate(source));
        jdbc.update("UPDATE PEACH_FLYWAY_HISTORY SET CHECKSUM=CHECKSUM-1 WHERE VERSION='1'");
        var failure=PeachDatabaseAutoConfiguration.configure(Flyway.configure().dataSource(source)).locations("classpath:db/peach/migration","classpath:db/peach/failure").load();
        assertThrows(Exception.class,()->new PeachMigrationStrategy().migrate(failure));assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_ROLE",Integer.class)>0);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY WHERE SUCCESS=0",Integer.class));
    }
    @Test void concurrentStartsShareSingleVersion() throws Exception {
        var source=source();try(var executor=Executors.newFixedThreadPool(2)) {var a=executor.submit(()->migrate(source));var b=executor.submit(()->migrate(source));a.get(90,TimeUnit.SECONDS);b.get(90,TimeUnit.SECONDS);}
        assertEquals(1,new JdbcTemplate(source).queryForObject("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY",Integer.class));flyway(source).validate();
    }
    @Test void bootRunsInitialVersionAndRejectsInsufficientPool() {
        var source=source();new org.springframework.boot.test.context.runner.ApplicationContextRunner().withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(PeachDatabaseAutoConfiguration.class,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class)).withBean(javax.sql.DataSource.class,()->source).run(context->assertNull(context.getStartupFailure()));
        try(var pool=new com.zaxxer.hikari.HikariDataSource()) {pool.setJdbcUrl(source.getUrl());pool.setUsername(username);pool.setPassword(password);pool.setMaximumPoolSize(1);assertThrows(Exception.class,()->new PeachMigrationStrategy().migrate(PeachDatabaseAutoConfiguration.configure(Flyway.configure().dataSource(pool)).load()));}
    }
}
