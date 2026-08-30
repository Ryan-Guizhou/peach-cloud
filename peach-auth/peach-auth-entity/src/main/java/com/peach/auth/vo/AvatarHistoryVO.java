package com.peach.auth.vo;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 头像历史视图。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */


@Data
@Schema(description = "头像历史视图")
public class AvatarHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2318309168424513872L;

    @Schema(description = "头像历史主键")
    private String avatarHistoryId;

    @Schema(description = "头像文件ID")
    private String fileId;

    @Schema(description = "头像访问地址")
    private String avatarUrl;

    @Schema(description = "头像排序号")
    private Integer sortNo;

    @Schema(description = "是否当前头像")
    private Integer isCurrent;

    @Schema(description = "创建时间")
    private String createdTime;
}
