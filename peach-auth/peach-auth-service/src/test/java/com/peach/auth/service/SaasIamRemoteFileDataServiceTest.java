package com.peach.auth.service;

import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.dto.SaasIamFileObjectCheckDTO;
import com.peach.auth.service.data.OrganizationScopeMatcher;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SaaS 文件行级授权必须在同一有效角色绑定内满足业务动作与数据策略。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@ExtendWith(MockitoExtension.class)
class SaasIamRemoteFileDataServiceTest {

    @Mock
    private TenantSessionService sessionService;

    @Mock
    private ScopedDataAuthorizationDao authorizationDao;

    @Mock
    private OrganizationScopeMatcher organizationScopeMatcher;

    @InjectMocks
    private SaasIamRemoteFileDataService service;

    @Test
    void shouldAllowOnlySameBindingTenantActionAndSelfPolicy() {
        SaasIamFileObjectCheckDTO request = descriptor("FILE.READ", "tenant", "app", "user");
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.READ"))
                .thenReturn(List.of(grant("TENANT", "SELF")));

        assertThat(service.decide("subject", request).isAllowed()).isTrue();
    }

    @Test
    void shouldRejectForeignTenantBeforeLookingUpGrants() {
        SaasIamFileObjectCheckDTO request = descriptor("FILE.READ", "foreign", "app", "user");
        when(sessionService.requireForToken("subject")).thenReturn(principal());

        assertThat(service.decide("subject", request).isAllowed()).isFalse();
        verifyNoInteractions(authorizationDao);
    }

    @Test
    void shouldRejectCrossOwnerEvenWithSameTenant() {
        SaasIamFileObjectCheckDTO request = descriptor("FILE.READ", "tenant", "app", "other-user");
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.READ"))
                .thenReturn(List.of(grant("TENANT", "SELF")));

        assertThat(service.decide("subject", request).isAllowed()).isFalse();
    }

    @Test
    void shouldRejectMissingDataPolicyEvenIfApiActionCouldBeAllowed() {
        SaasIamFileObjectCheckDTO request = descriptor("FILE.DOWNLOAD", "tenant", "app", "user");
        when(sessionService.requireForToken("subject")).thenReturn(principal());

        assertThat(service.decide("subject", request).isAllowed()).isFalse();
    }

    @Test
    void shouldRejectOrganizationBoundGrantWithoutValidOrganization() {
        SaasIamFileObjectCheckDTO request = descriptor("FILE.READ", "tenant", "app", "user");
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        RoleDataGrantVO grant = grant("ORG_TREE", "TENANT_ALL");
        grant.setBindingOrgRefId("other-org");
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.READ"))
                .thenReturn(List.of(grant));

        assertThat(service.decide("subject", request).isAllowed()).isFalse();
    }

    @Test
    void shouldAllowRestoreOnlyWithExplicitRestorePolicy() {
        SaasIamFileObjectCheckDTO request = descriptor("FILE.RESTORE", "tenant", "app", "user");
        request.setDeleted(true);
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.RESTORE"))
                .thenReturn(List.of(grant("TENANT", "SELF")));

        assertThat(service.decide("subject", request).isAllowed()).isTrue();
    }

    @Test
    void shouldRejectUnrecognizedFileActionBeforeTokenInspection() {
        SaasIamFileObjectCheckDTO request = descriptor("USER.DELETE", "tenant", "app", "user");
        assertThat(service.decide("subject", request).isAllowed()).isFalse();
        verifyNoInteractions(sessionService, authorizationDao);
    }

    private static TenantSessionContext principal() {
        return new TenantSessionContext("user", "tenant", "member", "app", 2L, 7L);
    }

    private static SaasIamFileObjectCheckDTO descriptor(String action, String tenant,
                                                        String app, String owner) {
        SaasIamFileObjectCheckDTO dto = new SaasIamFileObjectCheckDTO();
        dto.setPermissionCode(action);
        dto.setTenantRefId(tenant);
        dto.setAppRefId(app);
        dto.setObjectRefId("00000000000000000000000000000001");
        dto.setOwnerUserRefId(owner);
        return dto;
    }

    private static RoleDataGrantVO grant(String bindingScope, String dataScope) {
        RoleDataGrantVO grant = new RoleDataGrantVO();
        grant.setBindingRefId("binding");
        grant.setRoleRefId("role");
        grant.setPermissionRefId("permission");
        grant.setBindingScopeType(bindingScope);
        grant.setDataScope(dataScope);
        return grant;
    }
}
