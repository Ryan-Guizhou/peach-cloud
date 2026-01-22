package com.peach.captcha.service.impl;

import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.factory.CaptchaServiceFactory;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.keymanager.RedisKeyManage;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 15:54
 */
@Slf4j
public class DefaultCaptchaService extends AbstractCacheService{

    @Override
    public void init(Properties config) {
        for (String s : CaptchaServiceFactory.INSTANCES.keySet()) {
            if(CaptchaEnum.CaptchaServiceType.DEFAULT.getCode().equals(s)){
                continue;
            }
            getService(s).init(config);
        }
    }

    @Override
    public Response get(CaptchaVO captchaVO) {
        if (captchaVO == null || StringUtil.isEmpty(captchaVO.getCaptchaType())) {
            return Response.paramError("defaultCaptchaService get param error");
        }
        return getService(captchaVO.getCaptchaType()).get(captchaVO);
    }

    @Override
    public Response check(CaptchaVO captchaVO) {
        if (captchaVO == null
                || StringUtil.isEmpty(captchaVO.getCaptchaType())
                || StringUtil.isEmpty(captchaVO.getToken()) ) {
          return Response.paramError("defaultCaptchaService check param error");
        }
        return getService(captchaVO.getCaptchaType()).check(captchaVO);
    }

    @Override
    public Response verification(CaptchaVO captchaVO) {
        if (captchaVO == null || StringUtil.isEmpty(captchaVO.getCaptchaVerification())) {
            return Response.paramError("defaultCaptchaService verification param error");
        }
        try {
            String codeKey = String.format(RedisKeyManage.RUNNING_CAPTCHA_SECOND.getKey(), captchaVO.getCaptchaVerification());
            if (!CaptchaServiceFactory.getCaptchaCacheService(CACHE_TYPE).exists(codeKey)) {
                return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
            }
            //二次校验取值后，即刻失效
            CaptchaServiceFactory.getCaptchaCacheService(CACHE_TYPE).delete(codeKey);
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
        return Response.success();
    }

    private CaptchaService getService(String captchaType){
        return CaptchaServiceFactory.INSTANCES.get(captchaType);
    }
}
