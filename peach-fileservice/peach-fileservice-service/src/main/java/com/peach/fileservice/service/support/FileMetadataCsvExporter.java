package com.peach.fileservice.service.support;

import com.peach.fileservice.vo.FileRecordVO;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 对已通过同一角色行/字段权限过滤的文件元数据进行有界 CSV 编码。
 *
 * <p>不接受原始 FileRecordVO 数据表内容：必须先得到 Auth 字段投影结果。
 * 对 Excel 公式前缀进行中和；字段总数固定，不导出 bucket/objectKey/provider
 * 或租户/成员物理主键。任何缺少文件 ID 的行都拒绝生成文件。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
public final class FileMetadataCsvExporter {

    private static final int MAX_ROWS = 500;

    private FileMetadataCsvExporter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 将被授权的文件行编码为 UTF-8 CSV（无 BOM）。
     *
     * @param rows 已逐行完成 Auth 字段脱敏的 FileRecordVO
     * @return 有界 CSV 内容
     */
    public static byte[] encode(List<FileRecordVO> rows) {
        if (rows == null || rows.size() > MAX_ROWS) {
            throw new SecurityException("Protected CSV export exceeds safe limit");
        }
        StringBuilder csv = new StringBuilder(4096);
        csv.append("fileId,fileName,displayName,contentType,fileSize,fileExt,fileStatus,");
        csv.append("bizType,bizId,bizTag,remark,createdTime\n");
        for (FileRecordVO row : rows) {
            if (row == null || !StringUtils.hasText(row.getFileId())) {
                throw new SecurityException("Protected CSV record identity is unavailable");
            }
            appendRow(csv, row);
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder csv, FileRecordVO row) {
        appendValue(csv, row.getFileId());
        appendValue(csv, row.getFileName());
        appendValue(csv, row.getDisplayName());
        appendValue(csv, row.getContentType());
        appendValue(csv, row.getFileSize());
        appendValue(csv, row.getFileExt());
        appendValue(csv, row.getFileStatus());
        appendValue(csv, row.getBizType());
        appendValue(csv, row.getBizId());
        appendValue(csv, row.getBizTag());
        appendValue(csv, row.getRemark());
        appendValue(csv, row.getCreatedTime());
        csv.setLength(csv.length() - 1);
        csv.append('\n');
    }

    private static void appendValue(StringBuilder csv, Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String normalized = text.stripLeading();
        if (!normalized.isEmpty() && "=+-@\t\r".indexOf(normalized.charAt(0)) >= 0) {
            text = "'" + text;
        }
        csv.append('"');
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '"') {
                csv.append('"');
            }
            csv.append(text.charAt(i));
        }
        csv.append('"').append(',');
    }
}
