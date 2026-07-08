package com.peach.fileservice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 云存储对象列表返回视图.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@Schema(description = "云存储对象列表返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudStorageObjectPageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "路径")
    private String path;
    @Schema(description = "查询参数")
    private Boolean truncated;
    @Schema(description = "对象列表")
    private List<CloudStorageObjectNodeVO> items = new ArrayList<CloudStorageObjectNodeVO>();
}
