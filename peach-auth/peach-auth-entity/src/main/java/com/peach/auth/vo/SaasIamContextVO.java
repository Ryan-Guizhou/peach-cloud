package com.peach.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务端校验后返回的最小 SaaS 物理身份快照。
 *
 * <p>不包含登录凭证、密码摘要、角色列表、机构业务号和用户敏感资料。
 * 仅可由通过 Same-Token 的内部服务按需获取，不得缓存代替请求期版本核验。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaasIamContextVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 202611180903L;

    private String userRefId;

    private String tenantRefId;

    private String memberRefId;

    private String appRefId;

    private int year;

    private long memberVersion;

    private long authVersion;
}
