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
    Long increment(String key, long val);

    /**
     * 原子增加计数，首次写入时设置过期时间。
     *
     * @param key 计数键
     * @param val 增量
     * @param expiresInSeconds 首次写入的有效期
     * @return 当前计数
     */
    Long incrementAndExpire(String key, long val, long expiresInSeconds);

    /**
     * 原子读取并移除验证码，保证凭证只消费一次。
     *
     * @param key 验证码键
     * @return 已消费的值，不存在或过期返回 null
     */
    String getAndDelete(String key);


}
