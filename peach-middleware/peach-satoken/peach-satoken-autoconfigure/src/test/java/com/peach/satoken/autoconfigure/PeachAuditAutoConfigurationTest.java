package com.peach.satoken.autoconfigure;

import com.peach.common.audit.AuditContext;
import com.peach.common.audit.AuditContextProvider;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PeachAuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachAuditAutoConfiguration.class));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void registersDefaultAuditContextProvider() {
        UserContext userContext = new UserContext();
        userContext.setUserId("default-user");
        userContext.setTenantId("default-tenant");
        userContext.setOrgId("default-org");
        SecurityContextHolder.set(userContext);

        contextRunner.run(context -> {
            assertThat(AuditContext.currentUserId()).contains("default-user");
            assertThat(AuditContext.currentTenantId()).contains("default-tenant");
            assertThat(AuditContext.currentOrgId()).contains("default-org");
        });
    }

    @Test
    void registersCustomAuditContextProvider() {
        contextRunner
                .withBean(AuditContextProvider.class, CustomAuditContextProvider::new)
                .run(context -> assertThat(AuditContext.currentUserId()).contains("custom-user"));
    }

    private static final class CustomAuditContextProvider implements AuditContextProvider {

        @Override
        public String currentUserId() {
            return "custom-user";
        }

        @Override
        public String currentTenantId() {
            return "custom-tenant";
        }

        @Override
        public String currentOrgId() {
            return "custom-org";
        }
    }
}
