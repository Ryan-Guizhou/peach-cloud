package com.peach.auth.launch;

import com.peach.auth.dao.*;
import com.peach.auth.dto.*;
import com.peach.auth.entity.*;
import com.peach.auth.qo.MenuQO;
import com.peach.auth.service.impl.*;
import com.peach.auth.vo.*;
import com.peach.code.CodeGenerator;
import com.peach.common.PeachGroup;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * 四类业务编码生成、不可变性和菜单隔离回归。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/23 22:00
 */
class BusinessCodeTest {
    @Mock MenuDao menus;
    @Mock FunctionDao functions;
    @Mock RoleDao roles;
    @Mock RoleWorkspaceDao workspace;
    @Mock CodeGenerator generator;
    @InjectMocks MenuServiceImpl menuService;
    @InjectMocks FunctionServiceImpl functionService;
    @InjectMocks RoleServiceImpl roleService;
    AutoCloseable mocks;
    @BeforeEach void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        var actor = new UserContext(); actor.setTenantId("tenant"); actor.setOrgId("org"); actor.setUserId("actor");
        SecurityContextHolder.set(actor);
        when(generator.next("tenant", "MENU")).thenReturn("M0001");
        when(generator.next("tenant", "FUNCTION")).thenReturn("F0001");
        when(generator.next("tenant", "ROLE")).thenReturn("RL0001");
    }
    @AfterEach void cleanup() throws Exception { SecurityContextHolder.clear(); mocks.close(); }
    @Test void createsServerCodesAndIdsUsingTrustedContext() {
        var menu = new MenuDTO(); menu.setMenuCode("client"); menu.setMenuId("client-id");
        menuService.add(menu);
        verify(menus).insert(argThat((MenuDO row) -> "M0001".equals(row.getMenuCode()) && row.getMenuId().length()==32 && !"client-id".equals(row.getMenuId()) && "tenant".equals(row.getTenantId()) && Integer.valueOf(1).equals(row.getIsLeaf())));
        var function = new FunctionDTO(); function.setFuncCode("client"); function.setFuncId("client-id");
        functionService.add(function);
        verify(functions).insert(argThat((FunctionDO row) -> "F0001".equals(row.getFuncCode()) && row.getFuncId().length()==32 && "tenant".equals(row.getTenantId())));
        var role = new RoleDTO(); role.setRoleCode("client"); role.setTenantId("other"); role.setOrgId("other");
        roleService.add(role);
        verify(roles).insert(argThat((RoleDO row) -> "RL0001".equals(row.getRoleCode()) && row.getRoleId().length()==32 && "tenant".equals(row.getTenantId()) && "org".equals(row.getOrgId())));
    }
    @Test void menuCannotReadUpdateOrDeleteAnotherTenant() {
        var foreign = new MenuVO(); foreign.setTenantId("foreign");
        when(menus.selectById("foreign")).thenReturn(foreign);
        assertThrows(BusinessException.class, () -> menuService.selectById("foreign"));
        assertThrows(BusinessException.class, () -> menuService.delById("foreign"));
        var input = new MenuDTO(); input.setMenuId("foreign");
        assertThrows(BusinessException.class, () -> menuService.update(input));
        verify(menus, never()).delById(anyString()); verify(menus, never()).updateById(any());
    }
    @Test void menuPreservesCodeAndRejectsForeignParentOrFunction() {
        var existing = new MenuVO(); existing.setMenuId("id"); existing.setMenuCode("M0007"); existing.setTenantId("tenant"); existing.setFuncCode("F0002");
        when(menus.selectById("id")).thenReturn(existing); when(functions.count(any())).thenReturn(1);
        var update = new MenuDTO(); update.setMenuId("id"); menuService.update(update);
        verify(menus).updateById(argThat((MenuDO row) -> "M0007".equals(row.getMenuCode()) && "F0002".equals(row.getFuncCode())));
        update.setMenuCode("changed"); assertThrows(BusinessException.class, () -> menuService.update(update));
        var create = new MenuDTO(); create.setParentMenuId("foreign");
        assertThrows(BusinessException.class, () -> menuService.add(create));
        create.setParentMenuId(null); create.setFuncCode("missing"); when(functions.count(any())).thenReturn(0);
        assertThrows(BusinessException.class, () -> menuService.add(create));
        verify(menus, never()).insert(any());
    }
    @Test void menuQueriesAreScopedBeforeDaoUse() {
        var query = new MenuQO(); query.setTenantId("foreign");
        menuService.pageList(query);
        verify(menus).selectByQO(argThat(q -> "tenant".equals(q.getTenantId())));
    }
    @Test void insertValidationDoesNotRequireServerGeneratedCodesOrMenuId() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Object dto : new Object[]{new MenuDTO(),new FunctionDTO(),new RoleDTO(),new RouterDTO()}) {
                assertTrue(validator.validate(dto, PeachGroup.InsertGroup.class).stream().noneMatch(v -> v.getPropertyPath().toString().matches("menuId|menuCode|funcCode|roleCode|routerCode")));
            }
        }
    }

    @Test void roleUpdatePreservesOmittedCodeAndRejectsChanges() {
        var existing = new RoleVO(); existing.setRoleId("role"); existing.setRoleCode("RL0009"); existing.setTenantId("tenant"); existing.setOrgId("org");
        when(workspace.lockRole(eq("role"), any())).thenReturn(existing);
        var input = new RoleDTO(); input.setRoleId("role"); roleService.update(input);
        verify(roles).updateById(argThat((RoleDO row) -> "RL0009".equals(row.getRoleCode())));
        input.setRoleCode("changed"); assertThrows(BusinessException.class, () -> roleService.update(input));
        verifyNoInteractions(generator);
    }

    @Test void exhaustedSequencesDoNotInsertBusinessRows() {
        when(generator.next("tenant", "MENU")).thenThrow(new com.peach.code.CodeGeneratorException("Exhausted"));
        when(generator.next("tenant", "FUNCTION")).thenThrow(new com.peach.code.CodeGeneratorException("Exhausted"));
        when(generator.next("tenant", "ROLE")).thenThrow(new com.peach.code.CodeGeneratorException("Exhausted"));
        assertThrows(com.peach.code.CodeGeneratorException.class, () -> menuService.add(new MenuDTO()));
        assertThrows(com.peach.code.CodeGeneratorException.class, () -> functionService.add(new FunctionDTO()));
        assertThrows(com.peach.code.CodeGeneratorException.class, () -> roleService.add(new RoleDTO()));
        verify(menus, never()).insert(any()); verify(functions, never()).insert(any()); verify(roles, never()).insert(any());
    }

    @Test void menuMapperIncludesTenantPredicate() throws Exception {
        var configuration = new org.apache.ibatis.session.Configuration();
        String resource = "com/peach/auth/dao/MenuDao.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var query = new MenuQO(); query.setTenantId("tenant");
        var sql = configuration.getMappedStatement(MenuDao.class.getName()+".selectByQO").getBoundSql(query);
        var properties = sql.getParameterMappings().stream().map(org.apache.ibatis.mapping.ParameterMapping::getProperty).toList();
        assertTrue(properties.contains("tenantId")); assertFalse(properties.contains("appId"));
        var loaded = configuration.getMappedStatement(MenuDao.class.getName()+".selectById").getBoundSql("menu-id");
        assertTrue(loaded.getSql().contains("TENANT_ID as tenantId"));
        var inserted = configuration.getMappedStatement(MenuDao.class.getName()+".insert").getBoundSql(new MenuDO());
        assertTrue(inserted.getParameterMappings().stream().anyMatch(item -> "tenantId".equals(item.getProperty())));
    }

    @Test void invalidFunctionParentDoesNotConsumeASequence() {
        var input = new FunctionDTO(); input.setFuncCode("client"); input.setParentFuncCode("missing");
        assertThrows(BusinessException.class, () -> functionService.add(input));
        verifyNoInteractions(generator); verify(functions, never()).insert(any());
    }
}
