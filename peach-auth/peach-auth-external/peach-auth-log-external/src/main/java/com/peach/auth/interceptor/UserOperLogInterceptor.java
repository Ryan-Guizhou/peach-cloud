package com.peach.auth.interceptor;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.core.UserOperLogQueue;
import com.peach.auth.util.TransferUtil;
import com.peach.auth.vo.UserOperLogVO;
import com.peach.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * 用户操作审计拦截器，成功、业务失败与抛异常均以安全元数据记录。
 *
 * <p>审计采集不能影响业务方法的正常返回或原始异常传播；所有敏感输入和
 * 响应载荷在转换阶段不允许序列化或入队。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 13:50
 */
@Slf4j
public class UserOperLogInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        UserOperLog definition = AnnotationUtils.findAnnotation(method, UserOperLog.class);
        if (definition == null) {
            return invocation.proceed();
        }
        long start = System.currentTimeMillis();
        try {
            Object result = invocation.proceed();
            Response response = result instanceof Response businessResponse ? businessResponse : null;
            appendAudit(invocation, definition, start, response);
            return result;
        } catch (Throwable failure) {
            appendAudit(invocation, definition, start, null);
            throw failure;
        }
    }

    /**
     * 审计不可因自身转换或入队失败覆盖业务响应及异常。
     */
    private void appendAudit(MethodInvocation invocation, UserOperLog definition,
                             long start, Response response) {
        try {
            long duration = Math.max(0L, System.currentTimeMillis() - start);
            UserOperLogVO audit = TransferUtil.transferToOperLog(invocation, definition, duration, response);
            UserOperLogQueue.getInstance().addOperLogQueue(audit);
            log.debug("User operation audit queued, module={}, operation={}, elapsedMs={}",
                    definition.moduleCode(), definition.optType(), duration);
        } catch (RuntimeException exception) {
            log.warn("User operation audit capture failed, exceptionType={}",
                    exception.getClass().getSimpleName());
        }
    }
}
