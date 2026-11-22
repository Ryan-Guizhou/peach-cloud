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
    @Test void initializesFreshDatabaseAndDoesNotOverwriteCustomDataOnRestart() {
        var source=source(); migrate(source); var jdbc=new JdbcTemplate(source);
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_FUNCTION",Integer.class)>0);
        assertEquals(6,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_RESOURCE WHERE RESOURCE_CODE IN ('GET:/api/auth/role/workspace','GET:/api/auth/role/members','POST:/api/auth/role/saveGrants','POST:/api/auth/role/calibrate','POST:/api/auth/role/saveMembers','POST:/api/auth/role/saveDefault')",Integer.class));
        jdbc.update("UPDATE PEACH_ROLE SET ROLE_NAME='Custom name'");
        int count=jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY",Integer.class);
        migrate(source); assertEquals(count,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY",Integer.class));
        assertEquals("Custom name",jdbc.queryForObject("SELECT ROLE_NAME FROM PEACH_ROLE LIMIT 1",String.class));
        assertTrue(flyway(source).getConfiguration().isCleanDisabled());
        assertThrows(Exception.class,()->flyway(source).clean());
    }
    @Test void adoptsRecognizedLegacySchemaAndRetainsExistingData() {
        var source=source(); var jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE PEACH_USER (USER_ID VARCHAR(32) PRIMARY KEY,USER_CODE VARCHAR(50))");
        // Partial/noncanonical schemas must not be silently baselined.
        assertThrows(Exception.class,()->migrate(source));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='PEACH_FLYWAY_HISTORY'",Integer.class));
    }
    @Test void upgradesLegacySchemaAndPreservesIdentifiers() throws Exception {
        var source=source();var jdbc=new JdbcTemplate(source);
        try(var connection=source.getConnection()) {
            var context=org.mockito.Mockito.mock(org.flywaydb.core.api.migration.Context.class);
            org.mockito.Mockito.when(context.getConnection()).thenReturn(connection);
            new com.peach.database.migration.V1__Peach_schema().migrate(context);
        }
        jdbc.execute("DROP TABLE PEACH_TENANT");
        jdbc.execute("CREATE TABLE PEACH_TENANT(TENANT_ID VARCHAR(32) PRIMARY KEY,TENANT_CODE VARCHAR(64) NOT NULL UNIQUE,TENANT_NAME VARCHAR(128),STATUS VARCHAR(16),SORT_NUM INT,IS_DELETE INT,CREATED_TIME VARCHAR(20),CREATOR_ID VARCHAR(32),MODIFY_TIME VARCHAR(20),MODIFIER_ID VARCHAR(32))");
        jdbc.update("INSERT INTO PEACH_TENANT(TENANT_ID,TENANT_CODE,TENANT_NAME,STATUS) VALUES ('legacy-id','LEGACY','Retained tenant','DISABLE')");
        jdbc.execute("ALTER TABLE PEACH_ROUTER DROP COLUMN FUNC_CODE");
        jdbc.execute("ALTER TABLE PEACH_ROLE MODIFY SKIP_URL VARCHAR(32)");
        jdbc.execute("DROP TABLE PEACH_LOGIN_LOG");
        migrate(source);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_USER",Integer.class),"Adoption must not replay initial administrator or memberships");
        assertEquals("legacy-id",jdbc.queryForObject("SELECT ID FROM PEACH_TENANT WHERE TENANT_CODE='LEGACY'",String.class));
        assertEquals("SUSPENDED",jdbc.queryForObject("SELECT STATUS FROM PEACH_TENANT WHERE TENANT_CODE='LEGACY'",String.class));
        assertEquals("SYS_TENANT_MGMT",jdbc.queryForObject("SELECT FUNC_CODE FROM PEACH_ROUTER WHERE ROUTER_URL='/tenant'",String.class));
        assertEquals(100,jdbc.queryForObject("SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='PEACH_ROLE' AND COLUMN_NAME='SKIP_URL'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY WHERE TYPE='BASELINE'",Integer.class));
    }
    @Test void refusesUnrelatedDatabaseWithoutHistory() {
        var source=source(); var jdbc=new JdbcTemplate(source);jdbc.execute("CREATE TABLE UNRELATED(ID INT)");
        assertThrows(Exception.class,()->migrate(source));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()",Integer.class));
    }
    @Test void hikariPoolCapacityIsValidatedAndNormalPoolMigrates() {
        var source=source();
        try(var pool=new com.zaxxer.hikari.HikariDataSource()) {
            pool.setJdbcUrl(source.getUrl());pool.setUsername(username);pool.setPassword(password);pool.setMaximumPoolSize(1);
            var flyway=PeachDatabaseAutoConfiguration.configure(Flyway.configure().dataSource(pool)).load();
            assertTimeout(java.time.Duration.ofSeconds(3),()->assertThrows(Exception.class,()->new PeachMigrationStrategy().migrate(flyway)));
        }
        try(var pool=new com.zaxxer.hikari.HikariDataSource()) {
            pool.setJdbcUrl(source.getUrl());pool.setUsername(username);pool.setPassword(password);pool.setMaximumPoolSize(3);
            var flyway=PeachDatabaseAutoConfiguration.configure(Flyway.configure().dataSource(pool)).load();
            new PeachMigrationStrategy().migrate(flyway);flyway.validate();
        }
    }
    @Test void checksumMismatchAndSqlFailureBlockStartupWithoutCleaningData() {
        var source=source();migrate(source);var jdbc=new JdbcTemplate(source);
        jdbc.update("UPDATE PEACH_FLYWAY_HISTORY SET CHECKSUM=CHECKSUM+1 WHERE VERSION='5'");
        assertThrows(Exception.class,()->migrate(source));
        jdbc.update("UPDATE PEACH_FLYWAY_HISTORY SET CHECKSUM=CHECKSUM-1 WHERE VERSION='5'");
        var failure=PeachDatabaseAutoConfiguration.configure(Flyway.configure().dataSource(source)).locations("classpath:db/peach/migration","classpath:db/peach/failure").load();
        assertThrows(Exception.class,()->new PeachMigrationStrategy().migrate(failure));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_USER",Integer.class)>0);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY WHERE SUCCESS=0",Integer.class));
    }
    @Test void springBootAutomaticallyMigratesAndPropagatesFailures() {
        var source=source();
        var runner=new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(PeachDatabaseAutoConfiguration.class,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class))
                .withBean(javax.sql.DataSource.class,()->source);
        runner.run(context->{assertNull(context.getStartupFailure());assertNotNull(context.getBean("flywayInitializer"));assertTrue(new JdbcTemplate(source).queryForObject("SELECT COUNT(*) FROM PEACH_MENU",Integer.class)>0);});
        var unrelated=source();new JdbcTemplate(unrelated).execute("CREATE TABLE FOREIGN_TABLE(ID INT)");
        runner.withBean(javax.sql.DataSource.class,()->unrelated).run(context->assertNotNull(context.getStartupFailure()));
    }
    @Test void concurrentApplicationStartsShareOneMigrationHistory() throws Exception {
        var source=source();
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(()->migrate(source)); var second=executor.submit(()->migrate(source));
            first.get(90,TimeUnit.SECONDS);second.get(90,TimeUnit.SECONDS);
        }
        assertEquals(0,new JdbcTemplate(source).queryForObject("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY WHERE SUCCESS=0",Integer.class));
        flyway(source).validate();
    }
}
