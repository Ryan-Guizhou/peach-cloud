package com.peach.threadpool.core;

import java.util.concurrent.Callable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/4 19:16
 */
public class TaskWrapper {

    private final boolean enableSecurity;

    public TaskWrapper(boolean enableSecurity) {
        this.enableSecurity = enableSecurity;
    }

    public Runnable wrap(Runnable delegate) {
        Object sec = captureSecurity();
        return () -> {
            try {
                applySecurity(sec);
                delegate.run();
            } finally {
                clearSecurity();
            }
        };
    }

    public <V> Callable<V> wrap(Callable<V> delegate) {
        Object sec = captureSecurity();
        return () -> {
            try {
                applySecurity(sec);
                return delegate.call();
            } finally {
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
}
