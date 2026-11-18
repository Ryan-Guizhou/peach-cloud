package com.peach.auth.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 最小可信 API 动作判断结果；false 为唯一的故障默认值。
 *
 * <p>动作允许不代表对象行、文件和字段可访问。下游业务必须继续验证对象
 * 真实数据库权属及字段规则。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:20
 */
@Data
@NoArgsConstructor
public class SaasIamPermissionDecisionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 202611180902L;

    private boolean allowed;

    private SaasIamContextVO context;

    public SaasIamPermissionDecisionVO(boolean allowed) {
        this.allowed = allowed;
    }

    public SaasIamPermissionDecisionVO(boolean allowed, SaasIamContextVO context) {
        this.allowed = allowed;
        this.context = allowed ? context : null;
    }
}
