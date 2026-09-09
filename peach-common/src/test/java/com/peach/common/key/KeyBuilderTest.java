package com.peach.common.key;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyBuilderTest {

    private enum DemoKey implements KeyTemplate {
        USER_TOKEN("PEACH:AUTH:USER:{0}:TOKEN:{1}");

        private final String pattern;

        DemoKey(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String pattern() {
            return pattern;
        }
    }

    private enum MaintainedKey implements KeyDefinition {
        GLOBAL_LOCK("PEACH:COMMON:LOCK:{0}", "COMMON", "global lock key", "lock owner", "Peach");

        private final String pattern;

        private final String moduleCode;

        private final String keyIntroduce;

        private final String valueIntroduce;

        private final String author;

        MaintainedKey(String pattern, String moduleCode, String keyIntroduce, String valueIntroduce, String author) {
            this.pattern = pattern;
            this.moduleCode = moduleCode;
            this.keyIntroduce = keyIntroduce;
            this.valueIntroduce = valueIntroduce;
            this.author = author;
        }

        @Override
        public String pattern() {
            return pattern;
        }

        @Override
        public String moduleCode() {
            return moduleCode;
        }

        @Override
        public String keyIntroduce() {
            return keyIntroduce;
        }

        @Override
        public String valueIntroduce() {
            return valueIntroduce;
        }

        @Override
        public String author() {
            return author;
        }
    }

    @Test
    void shouldBuildKeyFromRawKey() {
        KeyBuilder key = KeyBuilder.of("PEACH:RAW:1");

        assertThat(key.getRealKey()).isEqualTo("PEACH:RAW:1");
    }

    @Test
    void shouldFormatKeyFromPattern() {
        KeyBuilder key = KeyBuilder.format("PEACH:SETTING:DICT:{0}", "gender");

        assertThat(key.getRealKey()).isEqualTo("PEACH:SETTING:DICT:gender");
    }

    @Test
    void shouldBuildKeyFromTemplate() {
        KeyBuilder key = KeyBuilder.from(DemoKey.USER_TOKEN, "u1", "login");

        assertThat(key.getRealKey()).isEqualTo("PEACH:AUTH:USER:u1:TOKEN:login");
    }

    @Test
    void shouldExposePublicKeyDefinitionMetadata() {
        KeyDefinition definition = MaintainedKey.GLOBAL_LOCK;

        assertThat(definition.pattern()).isEqualTo("PEACH:COMMON:LOCK:{0}");
        assertThat(definition.moduleCode()).isEqualTo("COMMON");
        assertThat(definition.keyIntroduce()).isEqualTo("global lock key");
        assertThat(definition.valueIntroduce()).isEqualTo("lock owner");
        assertThat(definition.author()).isEqualTo("Peach");
        assertThat(KeyBuilder.from(definition, "job-1").getRealKey()).isEqualTo("PEACH:COMMON:LOCK:job-1");
    }

    @Test
    void shouldRejectBlankKeyAndPattern() {
        assertThatThrownBy(() -> KeyBuilder.of(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("key must not be blank");
        assertThatThrownBy(() -> KeyBuilder.format(" ", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pattern must not be blank");
    }

    @Test
    void shouldRejectNullTemplate() {
        assertThatThrownBy(() -> KeyBuilder.from(null, "x"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("template must not be null");
    }

    @Test
    void shouldCompareByRealKey() {
        assertThat(KeyBuilder.of("PEACH:A")).isEqualTo(KeyBuilder.format("PEACH:{0}", "A"));
        assertThat(KeyBuilder.of("PEACH:A").hashCode()).isEqualTo(KeyBuilder.of("PEACH:A").hashCode());
    }
}