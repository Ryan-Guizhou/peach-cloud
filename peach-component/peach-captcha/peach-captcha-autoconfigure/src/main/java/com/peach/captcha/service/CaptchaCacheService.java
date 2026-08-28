package com.peach.captcha.service;

/**
 * 图片验证码缓存服务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:40
 * @Description 图片验证码缓存服务
 */
public interface CaptchaCacheService {

    /**
     * 设置
     * @param key 键
     * @param value 值
     * @param expiresInSeconds 过期时间
     * */
    void set(String key, String value, long expiresInSeconds);

    /**
     * 是否存在
     * @param key 键
     * @return 结果
     * */
    boolean exists(String key);

    /**
     * 删除
     * @param key 键
     * */
    void delete(String key);

    /**
     * 查询
     * @param key 键
     * @return 结果
     * */
    String get(String key);


    /***
     * 增加
     * @param key 键
     * @param val 值
     * @return 结果
     */
    default Long increment(String key, long val){
        return 0L;
    }


}
