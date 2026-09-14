package com.peach.captcha.quickstart.runner;

import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.quickstart.example.CaptchaCheckExample;
import com.peach.captcha.quickstart.example.CaptchaGenerateExample;
import com.peach.captcha.quickstart.example.CaptchaVerifyExample;
import com.peach.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示 TEXT 验证码的生成、成功校验与失败路径。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.captcha.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CaptchaDemoRunner implements ApplicationRunner {

    private final CaptchaGenerateExample generateExample;
    private final CaptchaCheckExample checkExample;
    private final CaptchaVerifyExample verifyExample;

    @Override
    public void run(ApplicationArguments args) {
        runGenerateAndPass();
        runReject();
        log.info("captcha demo finished");
    }

    private void runGenerateAndPass() {
        log.info("=== Captcha generate + check + verification demo ===");
        CaptchaVO generated = generateExample.generate("qs-pass");
        Response checkResp = checkExample.checkSuccess(generated);
        if (checkResp == null || !checkResp.isSuccess() || !(checkResp.getData() instanceof CaptchaVO checked)) {
            throw new IllegalStateException("captcha check success demo failed");
        }
        Response verifyResp = verifyExample.verify(checked.getCaptchaVerification());
        if (verifyResp == null || !verifyResp.isSuccess()) {
            throw new IllegalStateException("captcha verification demo failed");
        }
        log.info("pass flow finished, checkOk=true, verifyOk=true");
    }

    private void runReject() {
        log.info("=== Captcha reject demo ===");
        CaptchaVO wrongTarget = generateExample.generate("qs-wrong");
        Response wrongResp = checkExample.checkWrongAnswer(wrongTarget);
        if (wrongResp == null || wrongResp.isSuccess()) {
            throw new IllegalStateException("captcha wrong-answer demo failed");
        }

        CaptchaVO expiredTarget = generateExample.generate("qs-expired");
        Response expiredResp = checkExample.checkExpired(expiredTarget);
        if (expiredResp == null || expiredResp.isSuccess()) {
            throw new IllegalStateException("captcha expired demo failed");
        }

        Response missingResp = checkExample.checkMissingToken();
        if (missingResp == null || missingResp.isSuccess()) {
            throw new IllegalStateException("captcha missing-token demo failed");
        }
        log.info("reject flow finished, wrongCode={}, expiredCode={}, missingCode={}",
                wrongResp.getCode(), expiredResp.getCode(), missingResp.getCode());
    }
}
