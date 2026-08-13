package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "头像历史视图")
public class AvatarHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String avatarHistoryId;
    private String fileId;
    private String avatarUrl;
    private Integer sortNo;
    private Integer isCurrent;
    private String createdTime;
}
