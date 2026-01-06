package com.peach.threadpool.core;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/4 19:16
 */
@Slf4j
public class TaskWrapper {

    private final boolean enableMdc;

    private final boolean enableSecurity;

    public TaskWrapper(boolean enableMdc, boolean enableSecurity) {
        this.enableMdc = enableMdc;
        this.enableSecurity = enableSecurity;
    }

    public Runnable wrap(Runnable delegate) {
        Map<String, String> mdc = enableMdc ? MDC.getCopyOfContextMap() : null;
        Object sec = captureSecurity();
        return () -> {
            try {
                applyMdc(mdc);
                applySecurity(sec);
                delegate.run();
            } finally {
                clearMdc();
                clearSecurity();
            }
        };
    }

    public <V> Callable<V> wrap(Callable<V> delegate) {
        Map<String, String> mdc = enableMdc ? MDC.getCopyOfContextMap() : null;
        Object sec = captureSecurity();
        return () -> {
            try {
                applyMdc(mdc);
                applySecurity(sec);
                return delegate.call();
            } finally {
                clearMdc();
                clearSecurity();
            }
        };
    }

    private Object captureSecurity() {
        if (!enableSecurity) {
            return null;
        }
        try {
            Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = holder.getMethod("getContext").invoke(null);
            return context;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void applySecurity(Object context) {
        if (!enableSecurity || context == null) {
            return;
        }
        try {
            Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            holder.getMethod("setContext", Class.forName("org.springframework.security.core.context.SecurityContext")).invoke(null, context);
        } catch (Throwable ignored) {
        }
    }

    private void clearSecurity() {
        if (!enableSecurity) {
            return;
        }
        try {
            Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            holder.getMethod("clearContext").invoke(null);
        } catch (Throwable ignored) {
        }
    }

    private void applyMdc(Map<String, String> mdc) {
        if (!enableMdc) {
            return;
        }
        if (mdc == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(mdc);
        }
    }

    private void clearMdc() {
        if (enableMdc) {
            MDC.clear();
        }
    }
}
