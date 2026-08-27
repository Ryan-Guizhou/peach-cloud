package com.peach.openfeign.support;

import feign.Request;
import feign.RetryableException;

/**
 * 携带已分类异常的 Feign 重试异常。
 *
 * <p>Feign {@link feign.Retryer} 仅识别 {@link RetryableException} 子类型；本类必须继承
 * {@code RetryableException} 才能参与重试决策，继承链深度由 Feign 库定义（Sonar S110），
 * 无法在不动 Feign 契约的前提下消除。已在根 {@code pom.xml}
 * {@code sonar.issue.ignore.multicriteria} 中按文件忽略。</p>
 *
 * <p>业务语义异常作为 {@code cause} 传入，便于重试耗尽后保留 429、超时或服务不可用语义。</p>
 */
final class PeachFeignRetryableException extends RetryableException {

    PeachFeignRetryableException(int status, String message, Request.HttpMethod httpMethod, Request request,
                                 Throwable cause) {
        super(status, message, httpMethod, cause, (Long) null, request);
    }
}
