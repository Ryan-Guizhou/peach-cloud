package com.peach.auth.service.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 密码散列回归测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 15:35
 */
class PasswordHashSupportTest {

    private final PasswordHashSupport passwordHashSupport = new PasswordHashSupport();

    @Test
    void shouldStoreOnlySaltedOneWayHash() {
        String encoded = passwordHashSupport.encode("Example!StrongPassword42");
        assertThat(encoded).startsWith("$2");
        assertThat(encoded).isNotEqualTo("Example!StrongPassword42");
        assertThat(passwordHashSupport.matches("Example!StrongPassword42", encoded)).isTrue();
        assertThat(passwordHashSupport.matches("WrongPassword", encoded)).isFalse();
    }

    @Test
    void missingOrBlankPasswordMustNotAuthenticate() {
        assertThat(passwordHashSupport.matches("wrong", null)).isFalse();
        assertThat(passwordHashSupport.matches("wrong", "")).isFalse();
        assertThatThrownBy(() -> passwordHashSupport.encode(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
