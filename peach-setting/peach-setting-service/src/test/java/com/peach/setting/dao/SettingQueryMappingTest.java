package com.peach.setting.dao;

import com.peach.setting.entity.IpWhitelistDO;
import com.peach.setting.qo.IpWhitelistQO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 直接解析真实 Mapper 和查询对象，验证动态 SQL 属性契约。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/23 21:40
 */
class SettingQueryMappingTest {
    private static Configuration configuration;

    @BeforeAll static void parseMappers() throws Exception {
        configuration = new Configuration();
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:com/peach/setting/dao/*.xml");
        assertTrue(resources.length > 0);
        for (var resource : resources) {
            try (var stream = resource.getInputStream()) {
                new XMLMapperBuilder(stream, configuration, resource.toString(), configuration.getSqlFragments()).parse();
            }
        }
    }

    @TestFactory Stream<DynamicTest> queryObjectsMatchTheirMapperConditions() {
        return configuration.getMappedStatementNames().stream()
                .filter(name -> name.endsWith(".selectByQO"))
                .map(name -> DynamicTest.dynamicTest(name, () -> {
                    var statement = configuration.getMappedStatement(name);
                    var query = statement.getParameterMap().getType().getDeclaredConstructor().newInstance();
                    assertFalse(statement.getBoundSql(query).getSql().isBlank());
                }));
    }

    @Test void whitelistFiltersRetainScopeAndDisabledStatusWithoutAuditFields() {
        var query = new IpWhitelistQO();
        query.setId("entry"); query.setIpAddress("192.0.2.1"); query.setIpDesc("fixture");
        query.setStatus(0); query.setTenantId("tenant"); query.setOrgId("org");
        var sql = configuration.getMappedStatement("com.peach.setting.dao.IpWhitelistDao.selectByQO").getBoundSql(query);
        assertEquals(List.of("id", "ipAddress", "ipDesc", "status", "tenantId", "orgId"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertTrue(sql.getSql().contains("STATUS = ?"));
        assertTrue(sql.getSql().contains("ORDER BY MODIFY_TIME DESC, CREATED_TIME DESC"));
    }

    @Test void entityQueriesKeepAuditConditions() {
        var entity = new IpWhitelistDO();
        entity.setCreatedTime("2026-11-23 00:00:00");
        var sql = configuration.getMappedStatement("com.peach.setting.dao.IpWhitelistDao.count").getBoundSql(entity);
        assertTrue(sql.getParameterMappings().stream().anyMatch(mapping -> mapping.getProperty().equals("createdTime")));
    }
}
