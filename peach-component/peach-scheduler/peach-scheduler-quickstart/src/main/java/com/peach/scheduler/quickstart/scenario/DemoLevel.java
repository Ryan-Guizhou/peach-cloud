package com.peach.scheduler.quickstart.scenario;

/**
 * 由易到难的 Quickstart 演示级别。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:30
 */
public enum DemoLevel {

    /**
     * 入门：单能力最小闭环。
     */
    BASIC(1, "basic"),

    /**
     * 进阶：组合行为与常见路径。
     */
    INTERMEDIATE(2, "intermediate"),

    /**
     * 综合：贴近真实用法的多步骤编排。
     */
    ADVANCED(3, "advanced");

    private final int order;
    private final String code;

    DemoLevel(int order, String code) {
        this.order = order;
        this.code = code;
    }

    public int order() {
        return order;
    }

    public String code() {
        return code;
    }

    /**
     * @param code 配置中的级别编码
     * @return 匹配级别，未知时返回 null
     */
    public static DemoLevel fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toLowerCase();
        for (DemoLevel level : values()) {
            if (level.code.equals(normalized)) {
                return level;
            }
        }
        return null;
    }
}
