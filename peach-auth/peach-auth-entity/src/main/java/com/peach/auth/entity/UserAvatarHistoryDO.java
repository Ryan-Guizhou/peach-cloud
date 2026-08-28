package com.peach.auth.entity;

import java.io.Serial;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * 用户头像历史。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */


@Data
@Entity
@Table(name = "PEACH_USER_AVATAR_HISTORY")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户头像历史")
public class UserAvatarHistoryDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1707114200070052073L;

    @Id
    @Column(name = "AVATAR_HISTORY_ID")
    private String avatarHistoryId;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "FILE_ID")
    private String fileId;

    @Column(name = "SORT_NO")
    private Integer sortNo;

    @Column(name = "IS_CURRENT")
    private Integer isCurrent;

    @Column(name = "IS_DELETE")
    private Integer isDelete;
}
