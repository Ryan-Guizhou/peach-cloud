package com.peach.captcha.quickstart.runner;

import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.quickstart.scenario.CaptchaScenarioService;
import com.peach.common.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后演示一次验证码生成。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.captcha.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CaptchaDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CaptchaDemoRunner.class);

    private final CaptchaScenarioService scenarioService;

    /**
     * @param scenarioService 验证码场景服务
     */
    public CaptchaDemoRunner(CaptchaScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Response response = scenarioService.getCaptcha("demo-client");
        boolean hasToken = false;
        if (response != null && response.getData() instanceof CaptchaVO captchaVO) {
            hasToken = captchaVO.getToken() != null && !captchaVO.getToken().isBlank();
        }
        log.info("captcha quickstart finished, success={}, tokenPresent={}",
                response != null && response.isSuccess(), hasToken);
    }
}
