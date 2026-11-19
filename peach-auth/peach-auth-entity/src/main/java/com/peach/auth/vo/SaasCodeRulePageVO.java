package com.peach.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前可信租户编码规则的有界分页结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:15
 */
@Getter
@AllArgsConstructor
public class SaasCodeRulePageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 221002L;

    private final int page;

    private final int pageSize;

    private final boolean hasNext;

    private final List<SaasCodeRuleVO> items;
}
