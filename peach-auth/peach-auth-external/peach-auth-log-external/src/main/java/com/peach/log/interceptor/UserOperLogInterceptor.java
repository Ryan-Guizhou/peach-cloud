package com.peach.log.interceptor;

import com.peach.log.annoation.UserOperLog;
import com.peach.log.core.UserOperLogQueue;
import com.peach.log.util.TransferUtil;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import java.lang.reflect.Method;

/**
 * 操作日志拦截器：保留原业务结果与异常，记录失败分支，采集故障不覆盖业务返回。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Slf4j
public class UserOperLogInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getThis() == null ? invocation.getMethod()
                : AopUtils.getMostSpecificMethod(invocation.getMethod(), invocation.getThis().getClass());
        UserOperLog annotation = AnnotatedElementUtils.findMergedAnnotation(method, UserOperLog.class);
        if (annotation == null) annotation = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), UserOperLog.class);
        if (annotation == null) return invocation.proceed();
        long started = System.nanoTime();
        Object result = null;
        Throwable failure = null;
        try { result = invocation.proceed(); return result; }
        catch (Throwable ex) { failure = ex; throw ex; }
        finally {
            try {
                var record = TransferUtil.transferToOperLog(invocation, annotation, (System.nanoTime() - started) / 1_000_000, result, failure);
                if (record != null) UserOperLogQueue.getInstance().addOperLogQueue(record);
                else log.warn("Operation audit skipped because authenticated scope is unavailable, method={}", method.getName());
            } catch (RuntimeException ex) { log.error("Operation audit capture failed, errorType={}", ex.getClass().getSimpleName()); }
        }
    }
}
