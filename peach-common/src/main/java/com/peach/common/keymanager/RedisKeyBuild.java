package com.peach.common.keymanager;


import java.text.MessageFormat;
import java.util.Objects;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 11:14
 */
public final class RedisKeyBuild {

    private String realKey;

    public RedisKeyBuild(String key){
        this.realKey = key;
    }

    /**
     * 创建redis key
     * @param redisKeyManage
     * @param args
     * @return
     */
    public static RedisKeyBuild createRedisKey(RedisKeyManage redisKeyManage, Object... args){
        return new RedisKeyBuild(MessageFormat.format(redisKeyManage.getKey(), args));
    }

    public String getRealKey(){
        return realKey;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedisKeyBuild that = (RedisKeyBuild) o;
        return realKey.equals(that.realKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realKey);
    }

}
