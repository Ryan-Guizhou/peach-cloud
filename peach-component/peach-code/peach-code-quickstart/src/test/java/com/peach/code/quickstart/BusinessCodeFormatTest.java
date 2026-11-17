package com.peach.code.quickstart;

import com.peach.code.CodeFormat;
import com.peach.code.CodeGenerator;
import com.peach.code.CodeGeneratorException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 SaaS 平台业务编号的统一命名空间及安全格式规则。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 15:25
 */
class BusinessCodeFormatTest {

    @Test
    void platformScopeShouldBeIndependentOfTenant() {
        CodeGenerator generator = (tenantId, prefix) -> tenantId + ":" + prefix;
        assertThat(generator.nextGlobal("ORG")).isEqualTo("PLATFORM:ORG");
    }

    @Test
    void shouldPreviewAStableUppercaseCode() {
        CodeFormat format = new CodeFormat("org", "_", 6);
        assertThat(format.preview(12)).isEqualTo("ORG_000012");
        assertThat(new CodeFormat("role", "-", 4).preview(3)).isEqualTo("ROLE-0003");
    }

    @Test
    void shouldRejectUnsafeFormatsAndOverflow() {
        assertThatThrownBy(() -> new CodeFormat("A_B", "_", 4))
                .isInstanceOf(CodeGeneratorException.class);
        assertThatThrownBy(() -> new CodeFormat("ORG", "/", 4))
                .isInstanceOf(CodeGeneratorException.class);
        assertThatThrownBy(() -> new CodeFormat("ORG", "_", 0))
                .isInstanceOf(CodeGeneratorException.class);
        assertThatThrownBy(() -> new CodeFormat("ORG", "_", 2).preview(100))
                .isInstanceOf(CodeGeneratorException.class);
        assertThatThrownBy(() -> new CodeFormat("ORG", "_", 2).preview(0))
                .isInstanceOf(CodeGeneratorException.class);
    }
}
