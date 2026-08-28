package com.peach.common;

import org.springframework.stereotype.Indexed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Peach通用启动配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/13 13:07
 */
@Slf4j
@Indexed
@Configuration
@ComponentScan("com.peach.common")
public class PeachCommonStarter {
}
