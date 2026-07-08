package com.peach.monitor.rest.external;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 13:42
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/external/monitor")
@Tag(name = "MonitorExternalController", description = "暴露给外部的监控服务接口")
public class MonitorExternalController {


}
