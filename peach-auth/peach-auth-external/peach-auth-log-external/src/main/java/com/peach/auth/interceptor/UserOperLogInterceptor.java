package com.peach.userservice.interceptor;

import com.peach.common.response.Response;
import com.peach.userservice.util.TransferUtil;
import com.peach.userservice.core.UserOperLogQueue;
import com.peach.userservice.vo.UserOperLogVO;
import com.peach.userservice.annoation.UserOperLog;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 16:40
 */
@Slf4j
public class UserOperLogInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Method method = invocation.getMethod();
        UserOperLog userOperLog = AnnotationUtils.findAnnotation(method, UserOperLog.class);
        if (userOperLog == null){
            return invocation.proceed();
        }
        Object proceed = invocation.proceed();
        Response response = (Response) proceed;
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - start;
        UserOperLogVO userOperLogVO = TransferUtil.transferToOperLog(invocation, userOperLog, totalTime, response);
        UserOperLogQueue.getInstance().addOperLogQueue(userOperLogVO);
        log.info("用户操作日志,耗时: {}ms", userOperLog, totalTime);
        return proceed;
    }

}
