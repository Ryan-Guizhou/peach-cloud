package com.peach.rocket.idempotent;

/**
 * 消费幂等认领结果。只有 ACQUIRED 持有者才允许进入业务 Handler。
 *
 * @param status 认领结果
 * @param ownerToken 本次认领的唯一令牌，未获得执行权时为空
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
public record MqIdempotentClaim(Status status, String ownerToken) {

    public enum Status {
        ACQUIRED, IN_PROGRESS, SUCCEEDED
    }

    public static MqIdempotentClaim acquired(String ownerToken) {
        return new MqIdempotentClaim(Status.ACQUIRED, ownerToken);
    }

    public static MqIdempotentClaim inProgress() {
        return new MqIdempotentClaim(Status.IN_PROGRESS, null);
    }

    public static MqIdempotentClaim succeeded() {
        return new MqIdempotentClaim(Status.SUCCEEDED, null);
    }
}
