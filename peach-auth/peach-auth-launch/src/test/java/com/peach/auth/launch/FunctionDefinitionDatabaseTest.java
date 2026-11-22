package com.peach.auth.launch;

import com.peach.auth.dao.FunctionDao;
import com.peach.auth.dao.ResourceDao;
import com.peach.auth.dto.FunctionDTO;
import com.peach.auth.dto.ResourceDTO;
import com.peach.auth.qo.FunctionQO;
import com.peach.auth.qo.ResourceQO;
import com.peach.auth.service.impl.FunctionServiceImpl;
import com.peach.auth.service.impl.ResouceServceImpl;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 在真实 MySQL 上验证功能树与资源的增删改、筛选、关联保护；所有写入最终回滚。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 12:00
 */
@EnabledIfEnvironmentVariable(named = "PEACH_FUNCTION_DB_SMOKE", matches = "true")
class FunctionDefinitionDatabaseTest {
    @Test void definitionAndResourceWorkflowRollsBack() throws Exception {
        String address = System.getenv().getOrDefault("NACOS_SERVER_ADDR", "127.0.0.1:8848");
        String namespace = System.getenv().getOrDefault("NACOS_NAMESPACE", "dba0fbb1-fe66-464e-afce-33ba45c43b2b");
        var uri = URI.create("http://" + address + "/nacos/v1/cs/configs?dataId=peach-datasource.yml&group=PEACH-CLOUD&tenant=" + namespace);
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        Map<?, ?> root = new Yaml().load(response.body());
        var hikari = (Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) root.get("spring")).get("datasource")).get("hikari");
        var config = new Configuration();
        var pagination = new com.github.pagehelper.PageInterceptor();
        var pageProperties = new java.util.Properties();
        pageProperties.setProperty("helperDialect", "mysql");
        pagination.setProperties(pageProperties);
        config.addInterceptor(pagination);
        for (String name : new String[]{"FunctionDao", "ResourceDao"}) {
            String resource = "com/peach/auth/dao/" + name + ".xml";
            try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
            }
        }
        try (var connection = DriverManager.getConnection(hikari.get("jdbc-url").toString(),
                hikari.get("username").toString(), hikari.get("password").toString())) {
            connection.setAutoCommit(false);
            try (var session = new SqlSessionFactoryBuilder().build(config).openSession(connection)) {
                UserContext user = new UserContext();
                user.setTenantId(UUID.randomUUID().toString().replace("-", "")); user.setUserId("smoke");
                SecurityContextHolder.set(user);
                try {
                    var functions = new FunctionServiceImpl(session.getMapper(FunctionDao.class));
                    var resources = new ResouceServceImpl(session.getMapper(ResourceDao.class), session.getMapper(FunctionDao.class));
                    String app = UUID.randomUUID().toString().replace("-", "");
                    FunctionDTO parent = new FunctionDTO();
                    parent.setAppId(app); parent.setFuncCode("ROOT"); parent.setFuncName("Root");
                    functions.add(parent);
                    FunctionDTO child = new FunctionDTO();
                    child.setAppId(app); child.setFuncCode("CHILD"); child.setFuncName("Child"); child.setParentFuncCode("ROOT");
                    functions.add(child);
                    var list = functions.list(new FunctionQO());
                    assertThat(list).hasSize(2);
                    var rootRow = list.stream().filter(row -> row.getFuncCode().equals("ROOT")).findFirst().orElseThrow();
                    var childRow = list.stream().filter(row -> row.getFuncCode().equals("CHILD")).findFirst().orElseThrow();
                    assertThrows(BusinessException.class, () -> functions.delById(rootRow.getFuncId()));
                    ResourceDTO button = new ResourceDTO();
                    button.setAppId(app); button.setFuncCode("CHILD"); button.setResourceCode("child:add");
                    button.setResourceName("Create child"); button.setResourceType("BUTTON");
                    resources.add(button);
                    ResourceQO query = new ResourceQO();
                    query.setAppId(app); query.setFuncCode("CHILD"); query.setKeyword("Create"); query.setResourceType("BUTTON");
                    var found = resources.pageList(query);
                    assertThat(found.getTotal()).isEqualTo(1);
                    var saved = found.getList().getFirst();
                    assertThat(resources.selectById(saved.getResourceId()).getTenantId()).isEqualTo(user.getTenantId());
                    assertThrows(BusinessException.class, () -> functions.delById(childRow.getFuncId()));
                    String authorizationId = UUID.randomUUID().toString().replace("-", "");
                    try (var insert = connection.prepareStatement("INSERT INTO PEACH_AUTH_RESOURCE (RESOURCE_ID,TENANT_ID,APP_ID,FUNC_CODE,RESOURCE_CODE,PARTY_CODE,ORG_ID,OP_TYPE,IS_DELETE,FISCAL) VALUES (?,?,?,'CHILD','child:add','smoke-role','smoke-org','BUTTON',0,2026)")) {
                        insert.setString(1, authorizationId); insert.setString(2, user.getTenantId()); insert.setString(3, app); insert.executeUpdate();
                    }
                    session.clearCache();
                    assertThrows(BusinessException.class, () -> resources.delById(saved.getResourceId()));
                    ResourceDTO recode = new ResourceDTO();
                    recode.setResourceId(saved.getResourceId()); recode.setResourceCode("changed");
                    assertThrows(BusinessException.class, () -> resources.update(recode));
                    try (var delete = connection.prepareStatement("DELETE FROM PEACH_AUTH_RESOURCE WHERE RESOURCE_ID = ?")) {
                        delete.setString(1, authorizationId); delete.executeUpdate();
                    }
                    session.clearCache();
                    button.setResourceId(saved.getResourceId()); button.setResourceName("Updated"); resources.update(button);
                    assertThat(resources.selectById(saved.getResourceId()).getResourceName()).isEqualTo("Updated");
                    query.setFuncCode("ROOT"); query.setKeyword(null);
                    assertThat(resources.list(query)).isEmpty();
                    resources.delById(saved.getResourceId());
                    child.setFuncId(childRow.getFuncId()); child.setParentFuncCode(""); functions.update(child);
                    assertThat(functions.selectById(childRow.getFuncId()).getParentFuncCode()).isEmpty();
                    functions.delById(rootRow.getFuncId()); functions.delById(childRow.getFuncId());
                    assertThat(functions.list(new FunctionQO())).isEmpty();
                } finally { connection.rollback(); SecurityContextHolder.clear(); }
            }
        }
    }
}
