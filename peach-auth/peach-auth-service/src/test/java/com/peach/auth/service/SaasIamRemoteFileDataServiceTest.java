package com.peach.auth.service;

import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.dto.SaasIamFileFieldReadDTO;
import com.peach.auth.dto.SaasIamFileFieldWriteDTO;
import com.peach.auth.dto.SaasIamFileObjectCheckDTO;
import com.peach.auth.service.data.OrganizationScopeDirectory;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.SaasIamFileQueryScopeVO;
import com.peach.auth.vo.ScopedFieldRuleVO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private OrganizationScopeDirectory organizationScopeMatcher;

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
    void shouldReturnOnlyExplicitlyApprovedAndMaskedFields() {
        SaasIamFileObjectCheckDTO object = descriptor("FILE.READ", "tenant", "app", "user");
        SaasIamFileFieldReadDTO request = new SaasIamFileFieldReadDTO();
        request.setObject(object);
        request.setFields(Map.of("fileId", "FILE0001",
                "fileName", "secret.pdf", "createdTime", "2026-11-19"));
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.READ"))
                .thenReturn(List.of(grant("TENANT", "SELF")));
        when(authorizationDao.selectFieldRules("tenant", "member", "app",
                "FILE.READ", "FILE_RECORD", List.of("binding")))
                .thenReturn(List.of(field("fileId", "ALLOW"), field("fileName", "MASK")));

        Map<String, Object> projected = service.projectFields("subject", request);

        assertThat(projected).containsEntry("fileId", "FILE0001")
                .containsEntry("fileName", "****")
                .doesNotContainKey("createdTime");
    }

    @Test
    void shouldDenyFieldReadWhenNoFieldPolicyExists() {
        SaasIamFileFieldReadDTO request = new SaasIamFileFieldReadDTO();
        request.setObject(descriptor("FILE.READ", "tenant", "app", "user"));
        request.setFields(Map.of("fileId", "FILE0001"));
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.READ"))
                .thenReturn(List.of(grant("TENANT", "SELF")));

        assertThat(service.projectFields("subject", request)).isEmpty();
    }

    private static ScopedFieldRuleVO field(String name, String readEffect) {
        ScopedFieldRuleVO field = new ScopedFieldRuleVO();
        field.setBindingRefId("binding");
        field.setRoleRefId("role");
        field.setPermissionRefId("permission");
        field.setFieldCode(name);
        field.setMaskStrategy("FULL");
        field.setReadEffect(readEffect);
        field.setWriteEffect("DENY");
        return field;
    }

    @Test
    void shouldReuseSingleBindingLookupForWholeBatchButRevalidateEachFile() {
        SaasIamFileFieldReadDTO first = new SaasIamFileFieldReadDTO();
        first.setObject(descriptor("FILE.LIST", "tenant", "app", "user"));
        first.setFields(Map.of("fileId", "FILE0001", "fileName", "secret-a.pdf"));
        SaasIamFileFieldReadDTO second = new SaasIamFileFieldReadDTO();
        second.setObject(descriptor("FILE.LIST", "tenant", "app", "user"));
        second.getObject().setObjectRefId("00000000000000000000000000000002");
        second.setFields(Map.of("fileId", "FILE0002", "fileName", "secret-b.pdf"));
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.LIST"))
                .thenReturn(List.of(grant("TENANT", "SELF")));
        when(authorizationDao.selectFieldRules("tenant", "member", "app",
                "FILE.LIST", "FILE_RECORD", List.of("binding")))
                .thenReturn(List.of(field("fileId", "ALLOW"), field("fileName", "MASK")));

        List<Map<String, Object>> visible = service.projectBatchFields(
                "subject", List.of(first, second));

        assertThat(visible).hasSize(2);
        assertThat(visible.get(0)).containsEntry("fileId", "FILE0001")
                .containsEntry("fileName", "****");
        assertThat(visible.get(1)).containsEntry("fileId", "FILE0002")
                .containsEntry("fileName", "****");
        verify(authorizationDao, times(1))
                .selectDataGrants("tenant", "member", "app", "FILE.LIST");
        verify(authorizationDao, times(1))
                .selectFieldRules("tenant", "member", "app",
                        "FILE.LIST", "FILE_RECORD", List.of("binding"));
    }

    @Test
    void shouldRejectBatchWithForeignTenantRatherThanReturnPartialResults() {
        SaasIamFileFieldReadDTO first = new SaasIamFileFieldReadDTO();
        first.setObject(descriptor("FILE.LIST", "tenant", "app", "user"));
        first.setFields(Map.of("fileId", "FILE0001"));
        SaasIamFileFieldReadDTO second = new SaasIamFileFieldReadDTO();
        second.setObject(descriptor("FILE.LIST", "foreign", "app", "user"));
        second.setFields(Map.of("fileId", "FILE0002"));
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.LIST"))
                .thenReturn(List.of(grant("TENANT", "SELF")));

        assertThat(service.projectBatchFields("subject", List.of(first, second))).isEmpty();
    }

    @Test
    void shouldPlanSelfOnlyFileListWithinCurrentPhysicalTenant() {
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.LIST"))
                .thenReturn(List.of(grant("TENANT", "SELF")));

        SaasIamFileQueryScopeVO scope = service.planListQuery("subject");

        assertThat(scope.isSelf()).isTrue();
        assertThat(scope.isTenantAll()).isFalse();
        assertThat(scope.getTenantRefId()).isEqualTo("tenant");
        assertThat(scope.getAppRefId()).isEqualTo("app");
        assertThat(scope.getOrganizationRefIds()).isEmpty();
    }

    @Test
    void shouldRequireSameBindingWritableMetadataPolicy() {
        SaasIamFileFieldWriteDTO request = editRequest("user");
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.EDIT"))
                .thenReturn(List.of(grant("TENANT", "SELF")));
        ScopedFieldRuleVO writable = field("displayName", "ALLOW");
        writable.setWriteEffect("ALLOW");
        when(authorizationDao.selectFieldRules("tenant", "member", "app",
                "FILE.EDIT", "FILE_RECORD", List.of("binding")))
                .thenReturn(List.of(writable));

        assertThat(service.checkWriteFields("subject", request).isAllowed()).isTrue();
    }

    @Test
    void shouldDenyWriteWhenDataPolicyAndFieldPolicyCannotBeJoined() {
        SaasIamFileFieldWriteDTO request = editRequest("different-owner");
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.EDIT"))
                .thenReturn(List.of(grant("TENANT", "SELF")));

        assertThat(service.checkWriteFields("subject", request).isAllowed()).isFalse();
    }

    @Test
    void shouldDenyWriteWhenAnyFieldNotExplicitlyAllowed() {
        SaasIamFileFieldWriteDTO request = editRequest("user");
        request.getChanges().put("remark", "must be denied");
        when(sessionService.requireForToken("subject")).thenReturn(principal());
        when(authorizationDao.selectDataGrants("tenant", "member", "app", "FILE.EDIT"))
                .thenReturn(List.of(grant("TENANT", "SELF")));
        ScopedFieldRuleVO writable = field("displayName", "ALLOW");
        writable.setWriteEffect("ALLOW");
        when(authorizationDao.selectFieldRules("tenant", "member", "app",
                "FILE.EDIT", "FILE_RECORD", List.of("binding")))
                .thenReturn(List.of(writable));

        assertThatThrownBy(
                        () -> service.checkWriteFields("subject", request))
                .isInstanceOf(SecurityException.class);
    }

    private static SaasIamFileFieldWriteDTO editRequest(String owner) {
        SaasIamFileFieldWriteDTO request = new SaasIamFileFieldWriteDTO();
        request.setObject(descriptor("FILE.EDIT", "tenant", "app", owner));
        request.setChanges(new LinkedHashMap<>(Map.of("displayName", "new name")));
        return request;
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
