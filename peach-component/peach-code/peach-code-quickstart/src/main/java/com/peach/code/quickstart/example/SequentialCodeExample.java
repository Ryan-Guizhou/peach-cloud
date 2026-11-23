package com.peach.code.quickstart.example;

import com.peach.code.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 同一租户+类型连续调用 {@link CodeGenerator#next(String, String)}，演示顺序递增发号。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class SequentialCodeExample {

    public static final String TENANT_ID = "T001";

    public static final String CODE_TYPE = "MENU";

    private final CodeGenerator codeGenerator;

    /**
     * 为默认菜单规则分配下一个编码。
     *
     * @return 本次分配的菜单编码
     */
    public String nextMenu() {
        String code = codeGenerator.next(TENANT_ID, CODE_TYPE);
        log.info("sequential next, tenant={}, type={}, code={}", TENANT_ID, CODE_TYPE, code);
        return code;
    }
}
