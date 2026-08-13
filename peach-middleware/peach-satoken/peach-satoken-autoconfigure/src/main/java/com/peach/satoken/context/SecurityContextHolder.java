package com.peach.satoken.context;

/**
 * 当前请求的已验真用户上下文持有者。
 * <p>
 * 基于 {@link ThreadLocal} 实现，用于在当前线程中存储和访问已登录用户的 {@link UserContext} 信息。
 * 适用于 Web 请求的单次生命周期内，在 Controller、Service、DAO 等各层之间传递用户身份数据，
 * 避免显式传参，简化代码，同时保证线程隔离，防止并发污染。
 * </p>
 * <p>
 * 使用方式：在拦截器或过滤器中通过 {@link #set(UserContext)} 存入当前用户信息，
 * 业务层通过 {@link #get()} 获取，请求结束后务必调用 {@link #clear()} 清理，防止内存泄漏。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
public final class SecurityContextHolder {

    /**
     * 线程本地变量，存储当前线程的用户上下文对象。
     */
    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    /**
     * 私有构造方法，防止外部实例化该类。
     * <p>该类为工具类，所有方法均为静态方法，无需实例化。</p>
     */
    private SecurityContextHolder() {
    }

    /**
     * 设置当前线程的用户上下文。
     *
     * @param context 用户上下文对象，通常为已验真的用户信息
     */
    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    /**
     * 获取当前线程的用户上下文。
     *
     * @return 当前线程存储的 {@link UserContext} 对象，若未设置则返回 {@code null}
     */
    public static UserContext get() {
        return HOLDER.get();
    }

    public static String currentUserId() {
        UserContext context = get();
        return context == null ? null : trimToNull(context.getUserId());
    }

    public static String currentTenantId() {
        UserContext context = get();
        return context == null ? null : trimToNull(context.getTenantId());
    }

    public static String currentOrgId() {
        UserContext context = get();
        return context == null ? null : trimToNull(context.getOrgId());
    }

    /**
     * 清除当前线程的用户上下文。
     * <p>建议在请求完成（如拦截器的 afterCompletion 或过滤器的 finally 块）中调用，
     * 以避免 ThreadLocal 内存泄漏。</p>
     */
    public static void clear() {
        HOLDER.remove();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
