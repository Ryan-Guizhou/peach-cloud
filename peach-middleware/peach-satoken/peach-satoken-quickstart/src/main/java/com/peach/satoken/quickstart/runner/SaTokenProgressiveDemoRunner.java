package com.peach.satoken.quickstart.runner;

import com.peach.satoken.quickstart.scenario.DemoLevel;
import com.peach.satoken.quickstart.scenario.ProgressiveDemo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按 basic → intermediate → advanced 顺序执行 SaToken 演示。
 *
 * <p>可通过 {@code quickstart.satoken.demo.levels} 裁剪级别。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:30
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "quickstart.satoken.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenProgressiveDemoRunner implements ApplicationRunner {

    private final List<ProgressiveDemo> demos;
    private final Set<DemoLevel> enabledLevels;

    public SaTokenProgressiveDemoRunner(
            List<ProgressiveDemo> demos,
            @Value("${quickstart.satoken.demo.levels:basic,intermediate,advanced}") String levels) {
        this.demos = demos;
        this.enabledLevels = parseLevels(levels);
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ProgressiveDemo> ordered = demos.stream()
                .filter(demo -> enabledLevels.contains(demo.level()))
                .sorted(Comparator.comparingInt((ProgressiveDemo demo) -> demo.level().order())
                        .thenComparingInt(ProgressiveDemo::order))
                .toList();

        log.info("satoken progressive demo start, levels={}, demos={}", enabledLevels, ordered.size());
        for (ProgressiveDemo demo : ordered) {
            log.info(">>> running {}", demo.title());
            demo.execute();
            log.info("<<< finished {}", demo.title());
        }
        log.info("satoken progressive demo finished");
    }

    private static Set<DemoLevel> parseLevels(String levels) {
        if (levels == null || levels.isBlank()) {
            return EnumSet.allOf(DemoLevel.class);
        }
        Set<DemoLevel> parsed = Arrays.stream(levels.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> DemoLevel.fromCode(s.toLowerCase(Locale.ROOT)))
                .filter(level -> level != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DemoLevel.class)));
        return parsed.isEmpty() ? EnumSet.allOf(DemoLevel.class) : parsed;
    }
}
