package com.peach.auth.service;

import com.peach.auth.dao.SaasIamDirectoryDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.IamPermissionCodes;
import com.peach.auth.vo.SaasIamMemberBriefVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 跨服务目录查询只允许当前真实租户完整管理权限，不公开敏感列。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:50
 */
class SaasIamRemoteDirectoryServiceTest {

    private TenantSessionService sessions;
    private ScopedPermissionService permissionService;
    private SaasIamDirectoryDao directoryDao;
    private SaasIamRemoteDirectoryService service;

    @BeforeEach
    void prepare() {
        sessions = mock(TenantSessionService.class);
        permissionService = mock(ScopedPermissionService.class);
        directoryDao = mock(SaasIamDirectoryDao.class);
        service = new SaasIamRemoteDirectoryService(sessions, permissionService, directoryDao);
        when(sessions.requireForToken("user-token")).thenReturn(
                new TenantSessionContext("user", "tenant", "member", "app", 2, 8));
    }

    @Test
    void shouldRejectUnauthorizedOrForeignDirectoryQueriesBeforeDao() {
        assertThatThrownBy(() -> service.members("user-token", List.of("target")))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.organizations("user-token", List.of("target")))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(directoryDao);
    }

    @Test
    void shouldOnlyQueryCurrentTenantForAuthorizedMemberLookup() {
        when(permissionService.hasTenantPermission("tenant", "member", "app",
                IamPermissionCodes.TENANT_MEMBER_MANAGE)).thenReturn(true);
        SaasIamMemberBriefVO result = new SaasIamMemberBriefVO();
        result.setId("target");
        when(directoryDao.selectActiveMembers("tenant", List.of("target")))
                .thenReturn(List.of(result));

        assertThat(service.members("user-token", List.of("target")))
                .extracting(SaasIamMemberBriefVO::getId).containsExactly("target");
        verify(directoryDao).selectActiveMembers("tenant", List.of("target"));
    }

    @Test
    void shouldRejectDuplicateOrOversizedBatchAndNeverIssueUnboundedQueries() {
        when(permissionService.hasTenantPermission("tenant", "member", "app",
                IamPermissionCodes.TENANT_MEMBER_MANAGE)).thenReturn(true);
        assertThatThrownBy(() -> service.members("user-token", List.of("target", "target")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.members("user-token",
                Collections.nCopies(101, "target")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.members("user-token", List.of())).isEmpty();
        verifyNoInteractions(directoryDao);
    }
}
