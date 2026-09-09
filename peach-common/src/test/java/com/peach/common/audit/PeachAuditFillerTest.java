package com.peach.common.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeachAuditFillerTest {

    @Test
    void shouldRejectNullArgumentsWithReadableMessage() {
        assertThatThrownBy(() -> PeachAuditFiller.fillCreate(null, new AuditSnapshot("u", "t", "o")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("entity must not be null");

        assertThatThrownBy(() -> PeachAuditFiller.fillCreate(new com.peach.common.PeachDO(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("snapshot must not be null");
    }
}
