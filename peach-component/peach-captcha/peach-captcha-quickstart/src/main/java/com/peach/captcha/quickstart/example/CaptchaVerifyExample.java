package com.peach.captcha.quickstart.example;

import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 注入 {@link CaptchaService#verification(CaptchaVO)}，演示后端二次校验。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class CaptchaVerifyExample {

    private final CaptchaService captchaService;

    /**
     * 使用 check 成功后下发的二次校验参数完成后端校验，令牌一次性失效。
     *
     * @param captchaVerification 前端 check 成功后带回的二次校验参数
     * @return 二次校验响应
     */
    public Response verify(String captchaVerification) {
        CaptchaVO request = new CaptchaVO();
        request.setCaptchaVerification(captchaVerification);
        Response response = captchaService.verification(request);
        log.info("captcha verification, ok={}", response != null && response.isSuccess());
        return response;
    }
}
