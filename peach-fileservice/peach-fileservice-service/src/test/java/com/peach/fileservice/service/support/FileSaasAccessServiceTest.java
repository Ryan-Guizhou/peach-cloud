package com.peach.fileservice.service.support;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.dto.SaasIamFileObjectCheckDTO;
import com.peach.auth.dto.SaasIamFileFieldReadDTO;
import java.util.Map;
import java.util.List;
import com.peach.fileservice.vo.FileRecordVO;
import com.peach.auth.openfeign.SaasIamFeignClient;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 最终用户身份必须来自在线 Feign 权限事实，旧业务租户字段不能提升权限。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@ExtendWith(MockitoExtension.class)
class FileSaasAccessServiceTest {

    @Mock
    private SaasIamFeignClient iamFeignClient;

    @InjectMocks
    private FileSaasAccessService service;

    @Test
    void shouldRequireLiveTrustedPermissionAndMappedTenant() {
        SaasIamContextVO context = new SaasIamContextVO("user", "tenant", "member", "app", 2L, 7L);
        when(iamFeignClient.checkApiPermission(eq("subject-token"), any(SaasIamPermissionCheckDTO.class)))
                .thenReturn(new SaasIamPermissionDecisionVO(true, context));
        context.setTenantBusinessId("TENANT_00000001");
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            FileSaasScope actual = service.requireCurrent("POST:/api/file/internal/upload/check");
            assertThat(actual.tenantRefId()).isEqualTo("tenant");
            assertThat(actual.appRefId()).isEqualTo("app");
            assertThat(actual.userRefId()).isEqualTo("user");
            assertThat(actual.memberRefId()).isEqualTo("member");
            assertThat(actual.tenantId()).isEqualTo("TENANT_00000001");
            verify(iamFeignClient).checkApiPermission(eq("subject-token"),
                    any(SaasIamPermissionCheckDTO.class));
        }
    }

    @Test
    void shouldRejectMissingUserTokenWithoutCallingRemote() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn(null);
            assertThatThrownBy(() -> service.requireCurrent("POST:/api/file/internal/upload"))
                    .isInstanceOf(SecurityException.class);
        }
        verifyNoInteractions(iamFeignClient);
    }

    @Test
    void shouldRejectDeniedApiActionWithoutResolvingTenant() {
        when(iamFeignClient.checkApiPermission(eq("subject-token"), any(SaasIamPermissionCheckDTO.class)))
                .thenReturn(new SaasIamPermissionDecisionVO(false));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            assertThatThrownBy(() -> service.requireCurrent("GET:/api/file/internal/FILE0001"))
                    .isInstanceOf(SecurityException.class);
        }
            }

    @Test
    void shouldRejectMissingPhysicalOwnerOrTenantMapping() {
        SaasIamContextVO context = new SaasIamContextVO("user", "tenant", "member", "app", 2L, 7L);
        when(iamFeignClient.requireContext("subject-token")).thenReturn(context);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            assertThatThrownBy(service::requireContext).isInstanceOf(SecurityException.class);
        }
    }


    @Test
    void shouldAuthorizeFileRowOnlyAfterLiveSameBindingDataPolicy() {
        FileSaasScope scope = new FileSaasScope("tenant", "app", "user", "member", "TENANT_00000001");
        FileRecordVO row = new FileRecordVO();
        row.setId("00000000000000000000000000000001");
        row.setTenantRefId("tenant");
        row.setAppRefId("app");
        row.setOwnerUserRefId("user");
        when(iamFeignClient.checkFileObjectPermission(eq("subject-token"),
                any(SaasIamFileObjectCheckDTO.class)))
                .thenReturn(new SaasIamPermissionDecisionVO(true));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            service.requireFileData("FILE.READ", scope, row);
        }
        verify(iamFeignClient).checkFileObjectPermission(eq("subject-token"),
                any(SaasIamFileObjectCheckDTO.class));
    }

    @Test
    void shouldDenyDataPolicyBeforeGrantingSignedDownload() {
        FileSaasScope scope = new FileSaasScope("tenant", "app", "user", "member", "TENANT_00000001");
        FileRecordVO row = new FileRecordVO();
        row.setId("00000000000000000000000000000001");
        row.setTenantRefId("tenant");
        row.setAppRefId("app");
        row.setOwnerUserRefId("user");
        when(iamFeignClient.checkFileObjectPermission(eq("subject-token"),
                any(SaasIamFileObjectCheckDTO.class)))
                .thenReturn(new SaasIamPermissionDecisionVO(false));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            assertThatThrownBy(() -> service.requireFileData("FILE.DOWNLOAD", scope, row))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Test
    void shouldRejectForgedTenantBeforeCallingAuthDataPolicy() {
        FileSaasScope scope = new FileSaasScope("tenant", "app", "user", "member", "TENANT_00000001");
        FileRecordVO row = new FileRecordVO();
        row.setId("00000000000000000000000000000001");
        row.setTenantRefId("tenant");
        row.setAppRefId("app");
        row.setOwnerUserRefId("different-user");
        row.setTenantRefId("foreign-tenant");

        assertThatThrownBy(() -> service.requireFileData("FILE.READ", scope, row))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(iamFeignClient);
    }

    @Test
    void shouldRedactUnapprovedMetadataFromFileDetails() {
        FileSaasScope scope = new FileSaasScope("tenant", "app", "user", "member", "TENANT_00000001");
        FileRecordVO raw = record(scope);
        raw.setFileName("secret.pdf");
        raw.setObjectId("object-internal");
        raw.setStorageProvider("storage-internal");
        raw.setBucketName("private-bucket");
        raw.setObjectKey("secret-path");
        when(iamFeignClient.projectFileFields(eq("subject-token"),
                any(SaasIamFileFieldReadDTO.class)))
                .thenReturn(Map.of("fileId", "FILE0001", "fileName", "****"));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            FileRecordVO visible = service.projectVisibleFields("FILE.READ", scope, raw);
            assertThat(visible.getFileId()).isEqualTo("FILE0001");
            assertThat(visible.getFileName()).isEqualTo("****");
            assertThat(visible.getObjectId()).isNull();
            assertThat(visible.getBucketName()).isNull();
            assertThat(visible.getObjectKey()).isNull();
        }
    }

    @Test
    void shouldRejectFieldProjectionWithoutExplicitReadableFileId() {
        FileSaasScope scope = new FileSaasScope("tenant", "app", "user", "member", "TENANT_00000001");
        when(iamFeignClient.projectFileFields(eq("subject-token"),
                any(SaasIamFileFieldReadDTO.class)))
                .thenReturn(Map.of("fileName", "report.pdf"));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            assertThatThrownBy(() -> service.projectVisibleFields("FILE.READ", scope, record(scope)))
                    .isInstanceOf(SecurityException.class);
        }
    }

    private static FileRecordVO record(FileSaasScope scope) {
        FileRecordVO row = new FileRecordVO();
        row.setId("00000000000000000000000000000001");
        row.setFileId("FILE0001");
        row.setTenantRefId(scope.tenantRefId());
        row.setAppRefId(scope.appRefId());
        row.setOwnerUserRefId(scope.userRefId());
        return row;
    }

    @Test
    void shouldBatchProjectMultipleFilesWithOneTrustedFeignRequest() {
        FileSaasScope scope = new FileSaasScope("tenant", "app", "user", "member", "TENANT_00000001");
        FileRecordVO first = record(scope);
        FileRecordVO second = record(scope);
        second.setId("00000000000000000000000000000002");
        second.setFileId("FILE0002");
        when(iamFeignClient.projectFileFieldsBatch(eq("subject-token"),
                anyList())).thenReturn(List.of(
                    Map.of("fileId", "FILE0001", "fileName", "first.pdf"),
                    Map.of("fileId", "FILE0002", "fileName", "second.pdf")));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            List<FileRecordVO> projected = service.projectVisiblePage(scope, List.of(first, second));
            assertThat(projected).hasSize(2);
            assertThat(projected.get(0).getFileName()).isEqualTo("first.pdf");
            assertThat(projected.get(1).getFileName()).isEqualTo("second.pdf");
        }
        verify(iamFeignClient).projectFileFieldsBatch(eq("subject-token"), anyList());
    }

    @Test
    void shouldFailClosedOnPartialBatchResults() {
        FileSaasScope scope = new FileSaasScope("tenant", "app", "user", "member", "TENANT_00000001");
        FileRecordVO first = record(scope);
        FileRecordVO second = record(scope);
        second.setId("00000000000000000000000000000002");
        second.setFileId("FILE0002");
        when(iamFeignClient.projectFileFieldsBatch(eq("subject-token"),
                anyList())).thenReturn(List.of(Map.of("fileId", "FILE0001")));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("subject-token");
            assertThatThrownBy(() -> service.projectVisiblePage(scope, List.of(first, second)))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Test
    void shouldRejectPathSegmentsOutsideBusinessIdGrammar() {
        assertThatThrownBy(() -> service.requireForFile("DELETE", "../OTHER", ""))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.requireForFile("GET", "FILE1", "/unknown"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(iamFeignClient);
    }
}
