package com.peach.auth.launch;

import com.peach.auth.dao.*;
import com.peach.auth.dto.*;
import com.peach.auth.entity.*;
import com.peach.auth.qo.*;
import com.peach.auth.service.impl.*;
import com.peach.auth.service.support.*;
import com.peach.auth.vo.*;
import com.peach.common.exception.BusinessException;
import com.peach.common.response.Response;
import com.peach.log.annoation.UserOperLog;
import com.peach.log.core.*;
import com.peach.log.dao.UserOperLogDao;
import com.peach.log.interceptor.UserOperLogInterceptor;
import com.peach.satoken.context.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import org.yaml.snakeyaml.Yaml;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.AdditionalAnswers.delegatesTo;

/**
 * 真实MySQL回归路由筛选与更新、角色授权与成员隔离、校准预览及AOP操作日志落库。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@EnabledIfEnvironmentVariable(named = "PEACH_ADMIN_DB_SMOKE", matches = "true")
class AdminWorkspaceDatabaseTest {
    /**
     * 使用真实AOP代理触发两种结果，凭据样例不得进入记录。
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/22 21:00
     */
    public static class Fixture {
        @UserOperLog(optContent = "'操作示例,对象:['+#p0+']'")
        public Response success(Object payload) { return Response.success(payload); }
        @UserOperLog(optContent = "失败操作示例")
        public Response failure() { throw new IllegalStateException("private payload"); }
    }
    @Test void workflowsAndAuditActuallyReachMysql() throws Exception {
        String address = System.getenv().getOrDefault("NACOS_SERVER_ADDR", "127.0.0.1:8848");
        String namespace = System.getenv().getOrDefault("NACOS_NAMESPACE", "dba0fbb1-fe66-464e-afce-33ba45c43b2b");
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://"+address+"/nacos/v1/cs/configs?dataId=peach-datasource.yml&group=PEACH-CLOUD&tenant="+namespace)).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());
        Map<?,?> data = new Yaml().load(response.body());
        var db = (Map<?,?>)((Map<?,?>)((Map<?,?>)data.get("spring")).get("datasource")).get("hikari");
        var source = new UnpooledDataSource("com.mysql.cj.jdbc.Driver",db.get("jdbc-url").toString(),db.get("username").toString(),db.get("password").toString());
        Configuration config = new Configuration(new Environment("test",new JdbcTransactionFactory(),source));
        var pagination = new com.github.pagehelper.PageInterceptor(); var props = new Properties(); props.setProperty("helperDialect","mysql"); pagination.setProperties(props); config.addInterceptor(pagination);
        var resolver = new PathMatchingResourcePatternResolver();
        for (String pattern : List.of("classpath*:com/peach/auth/dao/*.xml","classpath*:com/peach/userservice/dao/*.xml")) for (var resource : resolver.getResources(pattern)) {
            try (var stream = resource.getInputStream()) { new XMLMapperBuilder(stream,config,resource.toString(),config.getSqlFragments()).parse(); }
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);
        String tenant = uuid(), org = uuid();
        UserContext actor = new UserContext(); actor.setUserId("fixture"); actor.setUserCode("fixture"); actor.setTenantId(tenant); actor.setOrgId(org); actor.setFiscal("2026");
        SecurityContextHolder.set(actor);
        try (SqlSession session = factory.openSession(false)) {
            try {
                var functionDao = session.getMapper(FunctionDao.class); var resourceDao = session.getMapper(ResourceDao.class); var roleDao = session.getMapper(RoleDao.class);
                FunctionDO function = new FunctionDO(); function.setFuncId(uuid()); function.setTenantId(tenant); function.setAppId(AdminScope.APP_ID); function.setFuncCode("FIXTURE"); function.setFuncName("Fixture"); function.setIsDelete(0); function.setIsDisable(0); functionDao.insert(function);
                ResourceDO resource = new ResourceDO(); resource.setResourceId(uuid()); resource.setTenantId(tenant); resource.setAppId(AdminScope.APP_ID); resource.setFuncCode("FIXTURE"); resource.setResourceCode("fixture:read"); resource.setResourceName("Fixture read"); resource.setResourceType("BUTTON"); resource.setIsDelete(0); resourceDao.insert(resource);
                var routes = new RouterServiceImpl(session.getMapper(RouterDao.class),functionDao, (tenantId, type) -> "FIXTURE_ROUTE");
                RouterDTO route = new RouterDTO(); route.setRouterCode("FIXTURE_ROUTE"); route.setRouterName("Before"); route.setRouterUrl("/fixture/route"); route.setFilePath("system/role"); route.setModuleCode("fixture"); route.setFuncCode("FIXTURE"); routes.add(route);
                RouterQO query = new RouterQO(); query.setRouterUrl("fixture/route"); query.setModuleCode("fixture"); query.setRouterCode("FIXTURE");
                var found = routes.pageList(query); assertEquals(1,found.getTotal()); route.setRouterId(found.getList().getFirst().getRouterId());
                route.setRouterName("After"); routes.update(route); assertEquals("After",routes.selectById(route.getRouterId()).getRouterName());
                assertEquals(1,routes.selectById(route.getRouterId()).getIsAuth()); assertEquals(0,routes.selectById(route.getRouterId()).getIsCache());
                query.setRouterUrl("absent"); assertTrue(routes.list(query).isEmpty());
                var real = session.getMapper(RoleWorkspaceDao.class);
                RoleWorkspaceDao workspaceDao = mock(RoleWorkspaceDao.class,delegatesTo(real));
                doReturn(1).when(workspaceDao).hasPermission(any(),anyString());
                var roles = new RoleServiceImpl(roleDao,(tenantId, type) -> "RL0001",workspaceDao);
                RoleDTO create = new RoleDTO(); create.setRoleCode("FIXTURE_ROLE"); create.setRoleName("角色筛选样例"); create.setRoleType("CUSTOM"); roles.add(create);
                RoleQO roleQuery = new RoleQO(); roleQuery.setKeyword("筛选"); var rolePage = roles.pageList(roleQuery); assertEquals(1,rolePage.getTotal());
                RoleVO role = rolePage.getList().getFirst();
                var service = new RoleWorkspaceService(roleDao,workspaceDao,functionDao,resourceDao,mock(LoginPermissionCacheRefresher.class),mock(AuthLogSupport.class));
                var initial = service.get(role.getRoleId());
                RoleWorkspaceDTO grant = new RoleWorkspaceDTO(); grant.setRoleId(role.getRoleId()); grant.setRevision(initial.getRevision()); grant.setFuncCodes(List.of("FIXTURE")); grant.setResourceKeys(List.of(RoleWorkspaceService.key("FIXTURE","BUTTON","fixture:read")));
                service.saveGrants(grant);
                assertThrows(BusinessException.class,() -> service.saveGrants(grant),"Stale drafts must not overwrite new grants");
                var current = service.get(role.getRoleId()); assertEquals(List.of("FIXTURE"),current.getFuncCodes()); assertEquals(1,current.getResourceGrants().size());
                var orphan = new AuthResourceDO(); orphan.setResourceId(uuid()); orphan.setTenantId(tenant); orphan.setOrgId(org); orphan.setAppId(AdminScope.APP_ID); orphan.setFiscal(2026); orphan.setPartyCode(role.getRoleCode()); orphan.setFuncCode("FIXTURE"); orphan.setOpType("BUTTON"); orphan.setResourceCode("deleted"); real.insertResource(orphan);
                current=service.get(role.getRoleId()); grant.setRevision(current.getRevision()); grant.setResourceKeys(current.getResourceGrants().stream().map(r -> RoleWorkspaceService.key(r.getFuncCode(),r.getOpType(),r.getResourceCode())).toList());
                var preview=service.calibrate(grant); assertEquals(1,preview.getRemovedResourceKeys().size()); assertEquals(2,real.resources(role).size(),"Preview must not write");
                grant.setResourceKeys(grant.getResourceKeys().stream().filter(k -> !preview.getRemovedResourceKeys().contains(k)).toList()); service.saveGrants(grant);
                assertEquals(1,real.resources(role).size());
                assertThrows(BusinessException.class,() -> roles.delById(role.getRoleId()));
                // Only the target role's membership is replaced; another role is retained.
                for (String code : List.of(role.getRoleCode(),"OTHER_FIXTURE")) {
                    AuthPartyDO membership=new AuthPartyDO(); membership.setId(uuid()); membership.setTenantId(tenant); membership.setOrgId(org); membership.setFiscal(2026); membership.setRoleCode(code); membership.setRoleType("CUSTOM"); membership.setPartyCode("fixture-member"); real.insertMember(membership);
                }
                var memberState=service.members(role.getRoleId()); RoleWorkspaceDTO memberCommand=new RoleWorkspaceDTO(); memberCommand.setRoleId(role.getRoleId()); memberCommand.setRevision(memberState.getRevision()); memberCommand.setUserCodes(List.of()); service.saveMembers(memberCommand);
                RoleDO other=new RoleDO(); other.setTenantId(tenant); other.setOrgId(org); other.setFiscal(2026); other.setRoleCode("OTHER_FIXTURE"); assertEquals(List.of("fixture-member"),real.memberCodes(other));
                assertTrue(real.memberCodes(role).isEmpty());
                // Full interceptor -> queue -> consumer -> separate committed MySQL transaction.
                UserOperLogQueue.getInstance().getAllUserOperLog();
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("POST","/fixture/operation")));
                ProxyFactory proxy=new ProxyFactory(new Fixture()); proxy.setProxyTargetClass(true); proxy.addAdvice(new UserOperLogInterceptor()); Fixture target=(Fixture)proxy.getProxy();
                target.success(Map.of("password","private fixture value")); assertThrows(IllegalStateException.class,target::failure);
                var records=UserOperLogQueue.getInstance().getAllUserOperLog(); assertEquals(2,records.size());
                for(var record:records) { assertEquals(tenant,record.getTenantId()); assertEquals(org,record.getOrgId()); assertNull(record.getResponseData()); assertFalse(record.getOptContent().contains("private")); UserOperLogQueue.getInstance().addOperLogQueue(record); }
                var logger=new UserOperLogServiceImpl(session.getMapper(UserOperLogDao.class),factory); new UserOperLogTask(logger).handleUserOperLog(); logger.batchInsert(records);
                try (SqlSession read=factory.openSession()) {
                    var dao=read.getMapper(UserOperLogDao.class); var filter=new com.peach.log.entity.UserOperLogDO(); filter.setTenantId(tenant); filter.setOrgId(org);
                    assertEquals(2,dao.count(filter)); assertEquals(Set.of("Y","N"),records.stream().map(r -> dao.selectById(r.getId()).getIsSuccess()).collect(java.util.stream.Collectors.toSet()));
                } finally {
                    try(var connection=source.getConnection();var delete=connection.prepareStatement("DELETE FROM USER_OPER_LOG WHERE TENANT_ID=? AND ORG_ID=?")) { delete.setString(1,tenant);delete.setString(2,org);delete.executeUpdate(); }
                }
            } finally { session.rollback(); SecurityContextHolder.clear(); RequestContextHolder.resetRequestAttributes(); UserOperLogQueue.getInstance().getAllUserOperLog(); }
        }
    }
    private static String uuid() { return UUID.randomUUID().toString().replace("-",""); }
}
