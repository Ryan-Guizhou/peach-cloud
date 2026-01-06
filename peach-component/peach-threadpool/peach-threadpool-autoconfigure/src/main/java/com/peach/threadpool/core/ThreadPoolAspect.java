package com.peach.threadpool.core;

import com.peach.threadpool.annoation.AsyncExecuted;
import com.peach.threadpool.manager.ThreadPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 18:50
 */
@Slf4j
@Aspect
public class ThreadPoolAspect {

    private final ThreadPoolManager threadPoolManager;

    public ThreadPoolAspect(ThreadPoolManager threadPoolManager) {
        this.threadPoolManager = threadPoolManager;
    }

    @Pointcut("@annotation(com.peach.threadpool.annoation.AsyncExecuted)")
    public void pointcut() {

    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        AsyncExecuted threadPool = method.getAnnotation(AsyncExecuted.class);

        boolean async = threadPool.async();
        long timeout = threadPool.timeoutMs();
        Class<?> returnType = method.getReturnType();
        if (CompletableFuture.class.isAssignableFrom(returnType)) {
            return CompletableFuture.supplyAsync(()->{
                try {
                    Object result = pjp.proceed();
                    if (result instanceof CompletableFuture) {
                        CompletableFuture<Object> cf = (CompletableFuture<Object>) result;
                        return cf.join();
                    }
                    return result;
                }catch (Throwable e){
                    log.error(e.getMessage(),e);
                    throw new RuntimeException(e);
                }
            },null);
        }
        if (!async){
            return pjp.proceed();
        }
        Future<Object> f = threadPoolManager.get(threadPool.type()).submit(() -> {
            try {
                return pjp.proceed();
            } catch (Throwable e) {
                throw new ExecutionException(e);
            }
        });
        if (timeout > 0) {
            return f.get(timeout, TimeUnit.MILLISECONDS);
        }
        return f.get();
    }

}
