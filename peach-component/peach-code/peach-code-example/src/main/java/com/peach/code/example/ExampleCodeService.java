package com.peach.code.example;

import org.springframework.stereotype.Indexed;
import com.peach.code.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务编码生成示例服务。
 *
 * <p>示例通过 Spring 事务代理调用编码生成器，展示编码生成与业务写入应处于同一个事务边界。
 * 实际业务服务应采用同样的事务边界，不要直接在事务外调用 {@code CodeGenerator}。</p>
 */
@Indexed
@Service
public class ExampleCodeService {

    private final CodeGenerator codeGenerator;

    /**
     * 构造示例服务。
     *
     * @param codeGenerator 业务编码生成器
     */
    public ExampleCodeService(CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    /**
     * 生成菜单编码。
     *
     * @param tenantId 租户标识
     * @return 菜单编码
     */
    @Transactional(rollbackFor = Exception.class)
    public String createMenuCode(String tenantId) {
        return codeGenerator.next(tenantId, "MENU");
    }

    /**
     * 生成通知编码。
     *
     * @param tenantId 租户标识
     * @return 通知编码
     */
    @Transactional(rollbackFor = Exception.class)
    public String createNoticeCode(String tenantId) {
        return codeGenerator.next(tenantId, "NOTICE");
    }
}
