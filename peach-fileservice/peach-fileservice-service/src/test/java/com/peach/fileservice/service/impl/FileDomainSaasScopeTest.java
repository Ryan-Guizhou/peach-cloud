package com.peach.fileservice.service.impl;

import com.peach.fileservice.dao.FileObjectDao;
import com.peach.fileservice.config.FileDomainProperties;
import com.peach.fileservice.dao.FileRecordDao;
import com.peach.fileservice.dao.FileUploadSessionDao;
import com.peach.fileservice.dto.FileUploadCheckDTO;
import com.peach.fileservice.dto.FileMetadataPatchDTO;
import com.peach.fileservice.dto.FileBatchActionDTO;
import java.util.List;
import java.util.Map;
import com.peach.fileservice.service.IFileDomainService;
import com.peach.fileservice.service.support.FileSaasAccessService;
import com.peach.fileservice.service.support.FileSaasScope;
import com.peach.fileservice.vo.FileRecordVO;
import com.peach.service.MultiZoneStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 文件域服务不能仅凭文件业务编号访问其他租户、应用或所有者的数据。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@ExtendWith(MockitoExtension.class)
class FileDomainSaasScopeTest {

    private static final FileSaasScope SCOPE =
            new FileSaasScope("tenant-ref", "app-ref", "user-ref", "member-ref", "TENANT_00000001");

    @Mock
    private FileObjectDao fileObjectDao;

    @Mock
    private FileRecordDao fileRecordDao;

    @Mock
    private FileUploadSessionDao fileUploadSessionDao;

    @Mock
    private MultiZoneStorage multiZoneStorage;

    @Mock
    private FileDomainProperties properties;

    @Mock
    private ObjectProvider<IFileDomainService> self;

    @Mock
    private FileSaasAccessService saasAccessService;

    @InjectMocks
    private FileDomainServiceImpl domainService;

    @Test
    void shouldUsePhysicalScopeBeforeReadingFile() {
        FileRecordVO row = new FileRecordVO();
        row.setFileId("FILE00001");
        when(saasAccessService.requireForFile("GET", "FILE00001", "")).thenReturn(SCOPE);
        when(saasAccessService.requireContext()).thenReturn(SCOPE);
        when(fileRecordDao.selectScopedDetail("FILE00001", "tenant-ref", "app-ref"))
                .thenReturn(row);
        when(saasAccessService.projectVisibleFields("FILE.READ", SCOPE, row)).thenReturn(row);

        assertThat(domainService.selectByFileId("FILE00001").getFileId()).isEqualTo("FILE00001");
        verify(fileRecordDao).selectScopedDetail("FILE00001", "tenant-ref", "app-ref");
    }

    @Test
    void shouldNeverSignUrlBeforeTrustedAuthorization() {
        when(saasAccessService.requireForFile("GET", "FILE00001", "/url"))
                .thenThrow(new SecurityException("denied"));

        assertThatThrownBy(() -> domainService.getDownloadUrl("FILE00001"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(fileRecordDao, multiZoneStorage);
    }

    @Test
    void shouldRejectUnverifiedFileRowDataPolicyBeforeStorageAccess() {
        FileRecordVO row = new FileRecordVO();
        row.setFileId("FILE00001");
        row.setId("00000000000000000000000000000001");
        row.setTenantRefId("tenant-ref");
        row.setAppRefId("app-ref");
        row.setOwnerUserRefId("user-ref");
        row.setFileStatus("ACTIVE");
        row.setIsDelete(0);
        when(saasAccessService.requireForFile("GET", "FILE00001", "/url")).thenReturn(SCOPE);
        when(saasAccessService.requireContext()).thenReturn(SCOPE);
        when(fileRecordDao.selectScopedDetail("FILE00001", "tenant-ref", "app-ref"))
                .thenReturn(row);
        doThrow(new SecurityException("data denied"))
                .when(saasAccessService).requireFileData("FILE.DOWNLOAD", SCOPE, row);

        assertThatThrownBy(() -> domainService.getDownloadUrl("FILE00001"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(multiZoneStorage);
    }

    @Test
    void shouldRestrictInstantUploadLookupToCurrentTenantAppAndOwner() {
        FileUploadCheckDTO request = new FileUploadCheckDTO();
        request.setSha256("001122");
        request.setFileSize(2048L);
        request.setFileName("report.pdf");
        request.setBizType("report");
        when(saasAccessService.requireCurrent("POST:/api/file/internal/upload/check"))
                .thenReturn(SCOPE);

        assertThat(domainService.uploadCheck(request).getInstantUpload()).isFalse();
        verify(fileObjectDao).selectActiveBySha256AndSize(
                "001122", 2048L, "tenant-ref", "app-ref", "user-ref");
    }

    @Test
    void shouldUpdateOnlyAuthorizedMetadataWithScopedSql() {
        FileRecordVO row = new FileRecordVO();
        row.setId("00000000000000000000000000000001");
        row.setFileId("FILE00001");
        row.setTenantRefId("tenant-ref");
        row.setAppRefId("app-ref");
        row.setOwnerUserRefId("user-ref");
        row.setFileStatus("ACTIVE");
        row.setIsDelete(0);
        FileMetadataPatchDTO patch = new FileMetadataPatchDTO();
        patch.setChanges(Map.of("displayName", "revised.pdf"));
        when(saasAccessService.requireForFile("PATCH", "FILE00001", "/metadata")).thenReturn(SCOPE);
        when(saasAccessService.requireContext()).thenReturn(SCOPE);
        when(fileRecordDao.selectScopedDetail("FILE00001", "tenant-ref", "app-ref"))
                .thenReturn(row);
        when(saasAccessService.currentOperator()).thenReturn("member-ref");
        when(fileRecordDao.updateScopedMetadata(eq("FILE00001"), eq("tenant-ref"), eq("app-ref"),
                eq("user-ref"), isNull(), eq(patch.getChanges()), anyString(), eq("member-ref")))
                .thenReturn(1);

        domainService.updateMetadata("FILE00001", patch);

        verify(saasAccessService).requireWritableFileFields(SCOPE, row, patch.getChanges());
        verify(fileRecordDao).updateScopedMetadata(eq("FILE00001"), eq("tenant-ref"), eq("app-ref"),
                eq("user-ref"), isNull(), eq(patch.getChanges()), anyString(), eq("member-ref"));
    }

    @Test
    void shouldDenyMetadataPatchBeforeQueryingAnyRecord() {
        FileMetadataPatchDTO patch = new FileMetadataPatchDTO();
        patch.setChanges(Map.of("displayName", "revised.pdf"));
        when(saasAccessService.requireForFile("PATCH", "FILE00001", "/metadata"))
                .thenThrow(new SecurityException("denied"));

        assertThatThrownBy(() -> domainService.updateMetadata("FILE00001", patch))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(fileRecordDao);
    }

    @Test
    void shouldRejectDuplicateFileIdentifiersBeforeBatchAuthorization() {
        FileBatchActionDTO data = new FileBatchActionDTO();
        data.setFileIds(List.of("FILE00001", "FILE00001"));

        assertThatThrownBy(() -> domainService.batchLogicalDelete(data))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(saasAccessService, fileRecordDao);
    }

    @Test
    void shouldRejectBatchApiDenialBeforeLookingUpFileRecords() {
        FileBatchActionDTO data = new FileBatchActionDTO();
        data.setFileIds(List.of("FILE00001", "FILE00002"));
        when(saasAccessService.requireCurrent("POST:/api/file/internal/batch/delete"))
                .thenThrow(new SecurityException("denied"));

        assertThatThrownBy(() -> domainService.batchLogicalDelete(data))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(fileRecordDao);
    }

    @Test
    void shouldRejectBatchRestoreWithoutItsOwnApiPermission() {
        FileBatchActionDTO data = new FileBatchActionDTO();
        data.setFileIds(List.of("FILE00001"));
        when(saasAccessService.requireCurrent("POST:/api/file/internal/batch/restore"))
                .thenThrow(new SecurityException("denied"));

        assertThatThrownBy(() -> domainService.batchRestore(data))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(fileRecordDao);
    }

    @Test
    void shouldRejectForeignFileBeforeQueryingStorage() {
        when(saasAccessService.requireForFile("DELETE", "FILE00001", ""))
                .thenThrow(new SecurityException("denied"));

        assertThatThrownBy(() -> domainService.logicalDelete("FILE00001"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(fileRecordDao, fileObjectDao);
    }
}
