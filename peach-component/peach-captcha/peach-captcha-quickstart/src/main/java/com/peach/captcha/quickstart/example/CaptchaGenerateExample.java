package com.peach.captcha.quickstart.example;

import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 注入 {@link CaptchaService#get(CaptchaVO)}，生成 TEXT 验证码。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class CaptchaGenerateExample {

    public static final String TEXT_TYPE = CaptchaEnum.CaptchaServiceType.TEXT.getCode();

    private final CaptchaService captchaService;

    /**
     * 生成文本验证码，返回带 token 与图片的 {@link CaptchaVO}。
     *
     * @param clientUid 客户端标识，用于频控隔离；本样例已关闭频控
     * @return 生成结果
     */
    public CaptchaVO generate(String clientUid) {
        CaptchaVO request = new CaptchaVO();
        request.setClientUid(clientUid);
        request.setCaptchaType(TEXT_TYPE);
        Response response = captchaService.get(request);
        if (response == null || !response.isSuccess() || !(response.getData() instanceof CaptchaVO captcha)) {
            throw new IllegalStateException("captcha generate failed");
        }
        log.info("captcha generated, clientUid={}, hasToken={}, hasImage={}",
                clientUid, captcha.getToken() != null, captcha.getSlidingOriginalImageBase64() != null);
        return captcha;
    }
}
