package com.peach.sample.captcha;

import lombok.RequiredArgsConstructor;


import org.springframework.stereotype.Indexed;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.response.Response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 18:53
 */
@Indexed
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class CaptchaController {

        private final CaptchaService captchaService;

    @PostMapping("/get")
    public Response getCaptch(@RequestBody CaptchaVO captchaVO) {
        Response response = captchaService.get(captchaVO);
        return response;
    }

    @PostMapping("/check")
    public Response checkCaptch(@RequestBody CaptchaVO captchaVO) {
        Response response = captchaService.check(captchaVO);
        return response;
    }

}
