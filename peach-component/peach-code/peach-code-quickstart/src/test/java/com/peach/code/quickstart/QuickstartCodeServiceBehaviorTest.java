package com.peach.code.quickstart;

import com.peach.code.CodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 无 MySQL/Redis 依赖时验证事务入口会调用 CodeGenerator.next。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@ExtendWith(MockitoExtension.class)
class QuickstartCodeServiceBehaviorTest {

    @Mock
    private CodeGenerator codeGenerator;

    @InjectMocks
    private QuickstartCodeService quickstartCodeService;

    @Test
    void shouldCreateMenuCodeViaGenerator() {
        when(codeGenerator.next("T001", "MENU")).thenReturn("MENU_00000001");

        String code = quickstartCodeService.createMenuCode("T001");

        assertThat(code).isEqualTo("MENU_00000001");
        verify(codeGenerator).next("T001", "MENU");
    }

    @Test
    void shouldCreateNoticeCodeViaGenerator() {
        when(codeGenerator.next("T001", "NOTICE")).thenReturn("NOTICE_00000001");

        String code = quickstartCodeService.createNoticeCode("T001");

        assertThat(code).isEqualTo("NOTICE_00000001");
        verify(codeGenerator).next("T001", "NOTICE");
    }
}
