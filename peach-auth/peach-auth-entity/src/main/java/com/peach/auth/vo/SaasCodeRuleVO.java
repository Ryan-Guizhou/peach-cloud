package com.peach.auth.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务端按当前已授权命名空间读取的业务编码规则快照。
 *
 * <p>不向客户端暴露规则物理主键、租户命名空间及审计操作者；
 * 当前水位只是查询瞬间的值，不构成下一业务编号的预留。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:15
 */
@Data
public class SaasCodeRuleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 221001L;

    private String codePrefix;

    private String scopeType;

    private String codeSeparator;

    private Integer maxCodeWidth;

    private Long currentValue;

    private Long ruleVersion;

    private String status;
}
