package com.peach.code.quickstart.example;

import com.peach.code.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 按租户和前缀分别发号，演示序列隔离：不同租户或不同前缀互不影响。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class IsolatedCodeExample {

    private final CodeGenerator codeGenerator;

    /**
     * 为指定租户和前缀分配下一个编码。
     *
     * @param tenantId 租户标识
     * @param prefix 编码前缀
     * @return 本次分配的业务编码
     */
    public String next(String tenantId, String prefix) {
        String code = codeGenerator.next(tenantId, prefix);
        log.info("isolated next, tenant={}, prefix={}, code={}", tenantId, prefix, code);
        return code;
    }
}
