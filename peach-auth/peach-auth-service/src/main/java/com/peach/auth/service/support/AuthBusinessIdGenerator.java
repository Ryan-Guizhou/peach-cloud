package com.peach.auth.service.support;

import com.peach.code.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * SaaS 认证域永久业务标识生成入口。
 *
 * <p>以 MySQL 为唯一发号事实源；PLATFORM 序列不受租户删除与恢复影响。
 * 物理主键仍由 UniqueIdFacade 生成，与业务标识相互独立。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 14:50
 */
@Component
@RequiredArgsConstructor
public class AuthBusinessIdGenerator {

    private static final String PLATFORM_NAMESPACE = "PLATFORM";

    private final CodeGenerator codeGenerator;

    /**
     * 分配一个永久业务标识。
     *
     * @param type 受控业务编码类型
     * @return 平台级永久唯一业务标识
     */
    public String next(AuthBusinessIdType type) {
        Objects.requireNonNull(type, "type");
        return codeGenerator.next(PLATFORM_NAMESPACE, type.name());
    }
}
