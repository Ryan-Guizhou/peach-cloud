package com.peach.virtualthread.quickstart.web;

import com.peach.virtualthread.quickstart.VirtualThreadScenarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手工验证虚拟线程聚合场景的 HTTP 入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@RestController
@RequestMapping("/demo")
public class VirtualThreadDemoController {

    private final VirtualThreadScenarioService scenarioService;

    /**
     * @param scenarioService 虚拟线程场景服务
     */
    public VirtualThreadDemoController(VirtualThreadScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    /**
     * 触发 database/storage/remote 并行聚合。
     *
     * @return 聚合结果
     */
    @GetMapping("/aggregate")
    public String aggregate() throws Exception {
        return scenarioService.aggregate().get();
    }
}
