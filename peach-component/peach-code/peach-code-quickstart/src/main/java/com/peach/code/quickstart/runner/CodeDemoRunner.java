package com.peach.code.quickstart.runner;

import com.peach.code.quickstart.example.IsolatedCodeExample;
import com.peach.code.quickstart.example.SequentialCodeExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示 {@code CodeGenerator} 的顺序发号与租户/前缀隔离。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:55
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.code.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CodeDemoRunner implements ApplicationRunner {

    private final SequentialCodeExample sequentialCodeExample;
    private final IsolatedCodeExample isolatedCodeExample;

    @Override
    public void run(ApplicationArguments args) {
        runSequentialDemo();
        runIsolationDemo();
        log.info("code demo finished");
    }

    private void runSequentialDemo() {
        log.info("=== sequential CodeGenerator.next demo ===");
        String first = sequentialCodeExample.nextMenu();
        String second = sequentialCodeExample.nextMenu();
        log.info("sequential first={}, second={}", first, second);
    }

    private void runIsolationDemo() {
        log.info("=== tenant/prefix isolation demo ===");
        String t001Menu = isolatedCodeExample.next("T001", "MENU");
        String t001Notice = isolatedCodeExample.next("T001", "NOTICE");
        String t002Menu = isolatedCodeExample.next("T002", "MENU");
        log.info("isolation t001Menu={}, t001Notice={}, t002Menu={}", t001Menu, t001Notice, t002Menu);
    }
}
