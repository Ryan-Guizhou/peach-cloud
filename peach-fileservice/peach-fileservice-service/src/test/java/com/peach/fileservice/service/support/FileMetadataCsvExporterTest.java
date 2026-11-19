package com.peach.fileservice.service.support;

import com.peach.fileservice.vo.FileRecordVO;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 有界文件元数据 CSV 输出：字段允许后再导出，必须阻止公式注入和内部信息泄漏。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
class FileMetadataCsvExporterTest {

    @Test
    void shouldEscapeQuotedFieldsAndPreventSpreadsheetFormulaInjection() {
        FileRecordVO record = row("FILE-001");
        record.setFileName("=HYPERLINK(\"https://unsafe.example\",\"x\")");
        record.setDisplayName("  +cmd");
        record.setBizTag("@SUM(1,1)");
        record.setRemark("contains,\"quotes\"\nand newline");
        record.setBucketName("internal-bucket");
        record.setObjectKey("internal/path");
        record.setStorageProvider("private");

        String csv = new String(FileMetadataCsvExporter.encode(List.of(record)), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("fileId,fileName,displayName,contentType");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"https://unsafe.example\"\",\"\"x\"\")\"");
        assertThat(csv).contains("\"'  +cmd\"");
        assertThat(csv).contains("\"'@SUM(1,1)\"");
        assertThat(csv).contains("\"contains,\"\"quotes\"\"\nand newline\"");
        assertThat(csv).doesNotContain("internal-bucket", "internal/path", "private");
    }

    @Test
    void shouldNotIncludeFieldsRejectedByAuthProjection() {
        FileRecordVO approved = row("FILE-002");
        approved.setFileName("****");
        String csv = new String(FileMetadataCsvExporter.encode(List.of(approved)), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"FILE-002\",\"****\"");
        assertThat(csv).doesNotContain("null");
    }

    @Test
    void shouldRejectMissingFileIdentityOrTooManyRows() {
        assertThatThrownBy(() -> FileMetadataCsvExporter.encode(List.of(new FileRecordVO())))
                .isInstanceOf(SecurityException.class);
        List<FileRecordVO> rows = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            rows.add(row("FILE-" + i));
        }
        assertThatThrownBy(() -> FileMetadataCsvExporter.encode(rows))
                .isInstanceOf(SecurityException.class);
    }

    private static FileRecordVO row(String id) {
        FileRecordVO record = new FileRecordVO();
        record.setFileId(id);
        return record;
    }
}
