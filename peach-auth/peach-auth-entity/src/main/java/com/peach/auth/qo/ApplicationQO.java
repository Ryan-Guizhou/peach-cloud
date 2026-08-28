package com.peach.auth.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Application查询参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:07
  */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Application查询参数")
public class ApplicationQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -532882074770503363L;

}
