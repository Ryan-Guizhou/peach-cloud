package com.peach.auth.service.data;

import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.ScopedFieldRuleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证可信 Token、数据行、字段策略共同限制后端服务读写。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
class ScopedDataAuthorizationServiceTest {

    private ScopedDataAuthorizationDao dao;
    private ScopedDataAuthorizationService service;
    private RoleDataGrantVO grant;

    @BeforeEach
    void setUp() {
        TenantSessionService tenantSessionService = mock(TenantSessionService.class);
        dao = mock(ScopedDataAuthorizationDao.class);
        OrganizationScopeMatcher matcher = (tenant, root, candidate) -> root.equals(candidate);
        service = new ScopedDataAuthorizationService(tenantSessionService, dao, matcher);

        when(tenantSessionService.requireCurrent()).thenReturn(
                new TenantSessionContext("user", "tenant", "member", "app", 1L, 2L));
        grant = new RoleDataGrantVO();
        grant.setBindingRefId("binding");
        grant.setRoleRefId("role");
        grant.setPermissionRefId("permission");
        grant.setBindingScopeType("TENANT");
        grant.setDataScope("TENANT_ALL");
    }

    @Test
    void shouldDenyObjectWithoutConfiguredRowScope() {
        assertThatThrownBy(() -> service.requireObjectAction("DOC.READ", data("tenant")))
                .isInstanceOf(SecurityException.class);
        verify(dao).selectDataGrants("tenant", "member", "app", "DOC.READ");
    }

    @Test
    void shouldDenyCrossTenantObjectsBeforeLookingUpFields() {
        when(dao.selectDataGrants("tenant", "member", "app", "DOC.READ"))
                .thenReturn(List.of(grant));
        assertThatThrownBy(() -> service.readAuthorizedFields("DOC.READ", "DOC",
                data("foreign-tenant"), Map.of("secret", "sensitive")))
                .isInstanceOf(SecurityException.class);
        verify(dao, never()).selectFieldRules(eq("tenant"), eq("member"), eq("app"), eq("DOC.READ"), eq("DOC"), anyList());
    }

    @Test
    void shouldOnlyReturnFieldsAllowedByTheMatchingRoleBinding() {
        when(dao.selectDataGrants("tenant", "member", "app", "DOC.READ"))
                .thenReturn(List.of(grant));
        when(dao.selectFieldRules(eq("tenant"), eq("member"), eq("app"),
                eq("DOC.READ"), eq("DOC"), anyList()))
                .thenReturn(List.of(field("name", "ALLOW", "DENY"),
                        field("secret", "DENY", "DENY")));

        Map<String, Object> result = service.readAuthorizedFields("DOC.READ", "DOC",
                data("tenant"), Map.of("name", "Report", "secret", "classified"));

        assertThat(result).containsEntry("name", "Report").doesNotContainKey("secret");
        assertThatCode(() -> service.requireObjectAction("DOC.READ", data("tenant")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRefuseUnknownFieldOnWrite() {
        when(dao.selectDataGrants("tenant", "member", "app", "DOC.EDIT"))
                .thenReturn(List.of(grant));
        when(dao.selectFieldRules(eq("tenant"), eq("member"), eq("app"),
                eq("DOC.EDIT"), eq("DOC"), anyList()))
                .thenReturn(List.of(field("title", "ALLOW", "ALLOW")));

        assertThatThrownBy(() -> service.requireWritableFields(
                "DOC.EDIT", "DOC", data("tenant"),
                Map.of("title", "New title", "isAdmin", true)))
                .isInstanceOf(SecurityException.class);
        assertThatCode(() -> service.requireWritableFields(
                "DOC.EDIT", "DOC", data("tenant"), Map.of("title", "New title")))
                .doesNotThrowAnyException();
    }

    private static DataObjectDescriptor data(String tenant) {
        return new DataObjectDescriptor(tenant, "app", "object", "user", null, false);
    }

    private static ScopedFieldRuleVO field(String code, String readEffect, String writeEffect) {
        ScopedFieldRuleVO result = new ScopedFieldRuleVO();
        result.setBindingRefId("binding");
        result.setRoleRefId("role");
        result.setPermissionRefId("permission");
        result.setFieldCode(code);
        result.setReadEffect(readEffect);
        result.setWriteEffect(writeEffect);
        result.setMaskStrategy("FULL");
        return result;
    }
}
