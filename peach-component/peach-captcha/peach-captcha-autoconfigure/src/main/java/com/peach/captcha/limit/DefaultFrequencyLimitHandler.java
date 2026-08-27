package com.peach.captcha.limit;

import com.peach.captcha.constant.CaptchaConst;
import com.peach.captcha.constant.CaptchaEnum;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaCacheService;
import com.peach.common.keymanager.RedisKeyBuild;
import com.peach.common.keymanager.RedisKeyManage;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import com.peach.common.util.StringUtil;

import java.util.Objects;
import java.util.Properties;


/**
 * 验证码接口限流:
 *      客户端ClientUid 组件
 * 针对同一个客户端的请求，做如下限制:
 * get
 * 	 1分钟内check失败5次，锁定5分
 * 	 1分钟内不能超过120次。
 * check:
 *   1分钟内不超过600次
 * verify:
 *   1分钟内不超过600次
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class DefaultFrequencyLimitHandler implements FrequencyLimitHandler {

    /**
     * 配置 / config
     */
    private final Properties config;

    /**
     * 缓存服务 / cache service
     */
    private final CaptchaCacheService captchCacheService;

    public DefaultFrequencyLimitHandler(Properties config, CaptchaCacheService captchCacheService) {
        this.config = config;
        this.captchCacheService = captchCacheService;
    }
    /**
     * 验证码获取频率限制
     * 每分钟超过120次或者每分钟失败5次，锁定5分钟
     * @param captchaVO
     * @return
     */
    @Override
    public Response validateGet(CaptchaVO captchaVO) {
        if (!validateParams(captchaVO)){
            return Response.fail(StatusEnum.API_REQ_PARAM_ERROR);
        }
        final String getKey  = getClientCid(captchaVO, CaptchaEnum.CaptchaOpertionType.GET.getCode());
        final String lockKey = getClientCid(captchaVO, CaptchaEnum.CaptchaOpertionType.LOCK.getCode());
        final String failKey = getClientCid(captchaVO, CaptchaEnum.CaptchaOpertionType.FAIL.getCode());
        // 已锁定，直接失败
        if (captchCacheService.exists(lockKey)) {
            return Response.fail(StatusEnum.API_REQ_LOCK_GET_ERROR);
        }
        // ========= GET 请求频控 =========
        long getLimit = getLongConfig(
                CaptchaConst.REQ_GET_MINUTE_LIMIT,
                CaptchaConst.DEFAULT_REQ_GET_MINUTE_LIMIT
        );
        long getCount = incrementOrInit(getKey, 60);
        if (getCount > getLimit) {
            return Response.fail(StatusEnum.API_REQ_LOCK_GET_ERROR);
        }
        // ========= 失败次数校验 =========
        long failLimit = getLongConfig(
                CaptchaConst.REQ_GET_LOCK_FAIL,
                CaptchaConst.DEFAULT_REQ_GET_LOCK_LIMIT
        );
        String failCount = captchCacheService.get(failKey);
        if (Objects.isNull(failCount)) {
            return null;
        }
        if (Long.parseLong(failCount) > failLimit) {
            long lockSeconds = getLongConfig(
                    CaptchaConst.REQ_GET_LOCK_SECONDS,
                    CaptchaConst.DEFAULT_REQ_GET_LOCK_SECONDS
            );
            captchCacheService.set(lockKey, "1", lockSeconds);
            return Response.fail(StatusEnum.API_REQ_LOCK_GET_ERROR);
        }
        return null;
    }
    @Override
    public Response validateCheck(CaptchaVO captchaVO) {
        if (!validateParams(captchaVO)){
            return Response.fail(StatusEnum.API_REQ_PARAM_ERROR);
        }
        final String checkKey = getClientCid(captchaVO, CaptchaEnum.CaptchaOpertionType.CHECK.getCode());
        long checkLimit = getLongConfig(
                CaptchaConst.REQ_CHECK_MINUTE_LIMIT,
                CaptchaConst.DEFAULT_REQ_CHECK_MINUTE_LIMIT
        );
        long checkCount = incrementOrInit(checkKey, 60);
        if (checkCount > checkLimit) {
            return Response.fail(StatusEnum.API_REQ_LOCK_CHECK_ERROR);
        }
        return null;
    }
    @Override
    public Response validataVerify(CaptchaVO captchaVO) {
        if (!validateParams(captchaVO)){
            return Response.fail(StatusEnum.API_REQ_PARAM_ERROR);
        }
        final String verifyKey = getClientCid(captchaVO, CaptchaEnum.CaptchaOpertionType.VERIFY.getCode());
        long verifyLimit = getLongConfig(
                CaptchaConst.REQ_VERIFY_MINUTE_LIMIT,
                CaptchaConst.DEFAULT_REQ_VERIFY_MINUTE_LIMIT
        );
        long verifyCount = incrementOrInit(verifyKey, 60);
        if (verifyCount > verifyLimit) {
            return Response.fail(StatusEnum.API_REQ_LOCK_VERIFY_ERROR);
        }
        return null;
    }
    /**
     * 获取客户端uid
     * @param captchaVO
     * @param type
     * @return
     */
    private String getClientCid(CaptchaVO captchaVO, String type){
        return RedisKeyBuild
                .createRedisKey(RedisKeyManage.CAPTCHA_REQ_LIMIT,captchaVO.getClientUid(),type)
                .getRealKey();
    }
    /**
     * 获取key对应的值，不存在则初始化
     * @param key
     * @param expireSeconds
     * @return
     */
    private synchronized long incrementOrInit(String key, long expireSeconds) {
        String value = captchCacheService.get(key);
        if (value == null) {
            captchCacheService.set(key, "1", expireSeconds);
            return 1L;
        }
        return captchCacheService.increment(key, 1L);
    }
    /**
     * 获取配置值，不存在则返回默认值
     * @param key
     * @param defaultValue
     * @return
     */
    private synchronized long getLongConfig(String key, String defaultValue) {
        return Long.parseLong(config.getProperty(key, defaultValue));
    }
    /**
     * 校验参数是否合法
     * @param captchaVO
     * @return
     */
    private boolean validateParams(CaptchaVO captchaVO){
        return captchaVO != null && !StringUtil.isBlank(captchaVO.getClientUid());
    }

}