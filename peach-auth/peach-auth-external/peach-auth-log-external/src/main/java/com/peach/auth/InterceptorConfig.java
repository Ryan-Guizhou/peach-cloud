package com.peach.auth;


import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.interceptor.UserOperLogInterceptor;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 18:10
 */
@Indexed
@Component
public class InterceptorConfig {

    @Bean
    public Advisor userOperLogAdvisor() {
        UserOperLogInterceptor userOperLogInterceptor = new UserOperLogInterceptor();
        AnnotationMatchingPointcut pointcut = AnnotationMatchingPointcut.forMethodAnnotation(UserOperLog.class);
        return buildAdvisor(userOperLogInterceptor, pointcut);
    }

    private Advisor buildAdvisor(Advice advice, Pointcut pointcut) {
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
        advisor.setAdvice(advice);
        advisor.setPointcut(pointcut);
        return advisor;
    }
}
