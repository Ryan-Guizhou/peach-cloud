package com.peach.code.quickstart;

import com.peach.code.CodeGenerator;
import com.peach.code.CodeGeneratorException;
import com.peach.code.quickstart.example.IsolatedCodeExample;
import com.peach.code.quickstart.example.SequentialCodeExample;
import com.peach.code.quickstart.generator.InMemoryCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证公开 {@link CodeGenerator} 的顺序发号、租户/前缀隔离与非法参数拒绝。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@SpringBootTest(properties = "quickstart.code.demo.enabled=false")
class CodeCapabilityTest {

    @Autowired
    private SequentialCodeExample sequentialCodeExample;

    @Autowired
    private IsolatedCodeExample isolatedCodeExample;

    @Autowired
    private InMemoryCodeGenerator codeGenerator;

    @BeforeEach
    void reset() {
        codeGenerator.reset();
    }

    @Test
    void shouldAllocateSequentialCodesForSameTenantAndPrefix() {
        String first = sequentialCodeExample.nextMenu();
        String second = sequentialCodeExample.nextMenu();

        assertThat(first).isEqualTo("MENU_00000001");
        assertThat(second).isEqualTo("MENU_00000002");
    }

    @Test
    void shouldIsolateSequencesByTenantAndPrefix() {
        String t001Menu = isolatedCodeExample.next("T001", "MENU");
        String t001Notice = isolatedCodeExample.next("T001", "NOTICE");
        String t002Menu = isolatedCodeExample.next("T002", "MENU");

        assertThat(t001Menu).isEqualTo("MENU_00000001");
        assertThat(t001Notice).isEqualTo("NOTICE_00000001");
        assertThat(t002Menu).isEqualTo("MENU_00000001");
    }

    @Test
    void shouldRejectIllegalTenantOrPrefix() {
        assertThatThrownBy(() -> isolatedCodeExample.next("T001*", "MENU"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("tenantId");
        assertThatThrownBy(() -> isolatedCodeExample.next("T001", "MENU_X"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("prefix");
    }
}
