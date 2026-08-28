package com.peach.fileservice.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传预检查结果。
 * <p>用于文件上传前的预检查结果返回，包含秒传判断、文件业务ID及
 * 对象存储复用状态等信息，支持客户端根据预检查结果决定后续上传策略。</p>
 * <ul>
 * <li>{@code instantUpload} — 标识文件是否可秒传，服务端通过SHA256匹配已有文件</li>
 * <li>{@code fileId} — 秒传成功时返回已有文件的业务ID，客户端可直接引用</li>
 * <li>{@code objectReused} — 标识底层存储对象是否复用了已有对象，辅助资源管理决策</li>
 * </ul>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@Schema(description = "文件上传预检查结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadCheckVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7342062511875711400L;

    @Schema(description = "是否秒传")
    private Boolean instantUpload;

    @Schema(description = "业务文件ID")
    private String fileId;

    @Schema(description = "是否复用已有对象")
    private Boolean objectReused;
}
