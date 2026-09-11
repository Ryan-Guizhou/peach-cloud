package com.peach.captcha.quickstart.web;

import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.quickstart.scenario.CaptchaScenarioService;
import com.peach.common.response.Response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码生成与校验 REST 入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaDemoController {

    private final CaptchaScenarioService scenarioService;

    /**
     * @param scenarioService 验证码场景服务
     */
    public CaptchaDemoController(CaptchaScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    /**
     * 生成验证码。
     *
     * @param request 可选客户端标识
     * @return 验证码响应
     */
    @PostMapping("/get")
    public Response get(@RequestBody(required = false) CaptchaVO request) {
        String clientUid = request == null ? "qs-client" : request.getClientUid();
        if (clientUid == null || clientUid.isBlank()) {
            clientUid = "qs-client";
        }
        return scenarioService.getCaptcha(clientUid);
    }

    /**
     * 校验验证码。
     *
     * @param request 校验参数
     * @return 校验响应
     */
    @PostMapping("/check")
    public Response check(@RequestBody CaptchaVO request) {
        return scenarioService.checkCaptcha(request);
    }
}
