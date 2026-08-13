package com.peach.setting.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.setting.entity.IpWhitelistDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * IP 白名单视图对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "IP白名单VO")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IpWhitelistVO extends IpWhitelistDO implements Serializable {

    private static final long serialVersionUID = 1L;
}
