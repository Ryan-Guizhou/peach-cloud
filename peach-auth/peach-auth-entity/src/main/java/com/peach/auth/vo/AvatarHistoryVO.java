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

    private String avatarHistoryId;
    private String fileId;
    private String avatarUrl;
    private Integer sortNo;
    private Integer isCurrent;
    private String createdTime;
}
