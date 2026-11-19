package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Auth 依据同一有效角色绑定的 DATA 行范围计算的文件列表 SQL 范围。
 *
 * <p>结果由受信 Same-Token 接口生成。每种范围在业务 SQL 中只参与
 * 参数化 WHERE 过滤，不能由前端请求内容替换。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Data
@Schema(description = "受信文件查询物理行范围")
public class SaasIamFileQueryScopeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 232003L;

    private String tenantRefId;

    private String appRefId;

    private String userRefId;

    private boolean tenantAll;

    private boolean self;

    private List<String> organizationRefIds = List.of();

    private List<String> selfOrganizationRefIds = List.of();
}
