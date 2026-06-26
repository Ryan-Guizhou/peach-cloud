package com.peach.autoconfigure;

import com.peach.PeachStorageAutoConfiguration;
import com.peach.service.MultiZoneStorage;
import com.peach.storage.StorageTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PeachStorageAutoConfigurationTest {

    @Test
    void shouldAutoConfigureStorageBeans() throws Exception {
        Path root = Files.createTempDirectory("peach-storage-test");
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PeachStorageAutoConfiguration.class))
                .withPropertyValues(
                        "peach.storage.enabled=true",
                        "peach.storage.primary=local",
                        "peach.storage.providers.local.type=LOCAL",
                        "peach.storage.providers.local.bucket-name=bucket",
                        "peach.storage.providers.local.root-path=" + root.toAbsolutePath()
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(StorageTemplate.class);
                    assertThat(context).hasSingleBean(MultiZoneStorage.class);
                });
    }

    @Test
    void shouldFailFastWhenPrimaryMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PeachStorageAutoConfiguration.class))
                .withPropertyValues(
                        "peach.storage.enabled=true",
                        "peach.storage.providers.local.type=LOCAL",
                        "peach.storage.providers.local.bucket-name=bucket",
                        "peach.storage.providers.local.root-path=./target/storage"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("peach.storage.primary");
                });
    }

    @Test
    void shouldFailFastWhenLocalProviderMissingRootPath() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PeachStorageAutoConfiguration.class))
                .withPropertyValues(
                        "peach.storage.enabled=true",
                        "peach.storage.primary=local",
                        "peach.storage.providers.local.type=LOCAL",
                        "peach.storage.providers.local.bucket-name=bucket"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("root-path");
                });
    }
}
