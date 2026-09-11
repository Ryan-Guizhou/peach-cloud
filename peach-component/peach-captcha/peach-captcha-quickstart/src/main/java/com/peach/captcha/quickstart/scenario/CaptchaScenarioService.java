package com.peach.captcha.quickstart.scenario;

import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.response.Response;
import org.springframework.stereotype.Service;

/**
 * 文本验证码生成与校验场景。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class CaptchaScenarioService {

    private final CaptchaService captchaService;

    /**
     * @param captchaService 验证码服务
     */
    public CaptchaScenarioService(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * 生成验证码。
     *
     * @param clientUid 客户端标识
     * @return 统一响应
     */
    public Response getCaptcha(String clientUid) {
        CaptchaVO vo = new CaptchaVO();
        vo.setClientUid(clientUid);
        vo.setCaptchaType("TEXT");
        return captchaService.get(vo);
    }

    /**
     * 校验验证码。
     *
     * @param request 校验请求
     * @return 统一响应
     */
    public Response checkCaptcha(CaptchaVO request) {
        if (request.getCaptchaType() == null) {
            request.setCaptchaType("TEXT");
        }
        return captchaService.check(request);
    }
}
