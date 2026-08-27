package com.peach.threadpool.core;

import io.micrometer.context.ContextSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 异步任务上下文包装器。
 *
 * <p>在任务提交线程捕获 Micrometer 上下文、MDC 和可选的 Spring Security 上下文，在工作
 * 线程执行期间恢复，并在任务结束后还原工作线程原有状态。该行为保证 requestId、traceId、
 * spanId 等关联字段不会因为线程切换丢失，也不会在线程复用时泄漏到后续任务。</p>
 */
@Slf4j
public class TaskWrapper {

    private static final String SECURITY_CONTEXT_HOLDER_CLASS = "org.springframework.security.core.context.SecurityContextHolder";


    private final boolean enableMdc;
    private final boolean enableSecurity;

    /**
     * 创建任务上下文包装器。
     *
     * @param enableMdc 是否传播 Micrometer ThreadLocal 和 MDC 上下文
     * @param enableSecurity 是否传播 Spring Security 上下文
     */
    public TaskWrapper(boolean enableMdc, boolean enableSecurity) {
        this.enableMdc = enableMdc;
        this.enableSecurity = enableSecurity;
    }

    /**
     * 包装 Runnable 任务。
     *
     * @param delegate 原始任务
     * @return 带上下文传播能力的任务
     */
    public Runnable wrap(Runnable delegate) {
        ContextSnapshot snapshot = captureContextSnapshot();
        Map<String, String> mdc = captureMdc();
        Object securityContext = captureSecurity();
        return () -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            Object previousSecurityContext = captureSecurity();
            try (ContextSnapshot.Scope ignored = setThreadLocals(snapshot)) {
                applyMdc(mdc);
                applySecurity(securityContext);
                delegate.run();
            } finally {
                restoreMdc(previousMdc);
                restoreSecurity(previousSecurityContext);
            }
        };
    }

    /**
     * 包装 Callable 任务。
     *
     * @param delegate 原始任务
     * @param <V> 返回值类型
     * @return 带上下文传播能力的任务
     */
    public <V> Callable<V> wrap(Callable<V> delegate) {
        ContextSnapshot snapshot = captureContextSnapshot();
        Map<String, String> mdc = captureMdc();
        Object securityContext = captureSecurity();
        return () -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            Object previousSecurityContext = captureSecurity();
            try (ContextSnapshot.Scope ignored = setThreadLocals(snapshot)) {
                applyMdc(mdc);
                applySecurity(securityContext);
                return delegate.call();
            } finally {
                restoreMdc(previousMdc);
                restoreSecurity(previousSecurityContext);
            }
        };
    }

    private ContextSnapshot captureContextSnapshot() {
        return enableMdc ? ContextSnapshot.captureAll() : null;
    }

    private ContextSnapshot.Scope setThreadLocals(ContextSnapshot snapshot) {
        return snapshot == null ? null : snapshot.setThreadLocals();
    }

    private Map<String, String> captureMdc() {
        return enableMdc ? MDC.getCopyOfContextMap() : null;
    }

    private void applyMdc(Map<String, String> context) {
        if (!enableMdc) {
            return;
        }
        if (context == null || context.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }

    private void restoreMdc(Map<String, String> context) {
        applyMdc(context);
    }

    private Object captureSecurity() {
        if (!enableSecurity) {
            return null;
        }
        try {
            Class<?> holder = Class.forName(SECURITY_CONTEXT_HOLDER_CLASS);
            return holder.getMethod("getContext").invoke(null);
        } catch (Exception ex) {
            log.warn("Failed to read Spring Security context", ex);
            return null;
        }
    }

    private void applySecurity(Object context) {
        if (!enableSecurity || context == null) {
            return;
        }
        try {
            Class<?> holder = Class.forName(SECURITY_CONTEXT_HOLDER_CLASS);
            holder.getMethod("setContext", Class.forName("org.springframework.security.core.context.SecurityContext")).invoke(null, context);
        } catch (Exception ex) {
            log.warn("Failed to apply Spring Security context", ex);
        }
    }

    private void restoreSecurity(Object context) {
        if (!enableSecurity) {
            return;
        }
        if (context != null) {
            applySecurity(context);
            return;
        }
        try {
            Class<?> holder = Class.forName(SECURITY_CONTEXT_HOLDER_CLASS);
            holder.getMethod("clearContext").invoke(null);
        } catch (Exception ex) {
            log.warn("Failed to clear Spring Security context", ex);
        }
    }
}
