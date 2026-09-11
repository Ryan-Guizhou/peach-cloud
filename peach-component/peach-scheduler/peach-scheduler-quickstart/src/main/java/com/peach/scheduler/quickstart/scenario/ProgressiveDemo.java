package com.peach.scheduler.quickstart.scenario;

/**
 * 可按级别编排的 Quickstart 演示单元。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:30
 */
public interface ProgressiveDemo {

    /**
     * @return 演示级别
     */
    DemoLevel level();

    /**
     * @return 同级别内顺序，越小越先执行
     */
    int order();

    /**
     * @return 演示标题，用于日志定位
     */
    String title();

    /**
     * 执行演示并完成断言性自检（失败抛异常）。
     */
    void execute();
}
