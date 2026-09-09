package com.peach.virtualthread.task;

/**
 * 活动虚拟任务安全快照。
 *
 * <p>快照只包含诊断需要的线程与时间信息，不包含 Runnable、Callable、业务参数、返回结果、
 * 用户信息或任何凭证。</p>
 *
 * @param taskId 任务 ID
 * @param group 业务组
 * @param taskState 任务状态
 * @param threadId 线程 ID，尚未绑定线程时为 0
 * @param threadName 线程名称，尚未绑定线程时为 null
 * @param virtual 是否为虚拟线程
 * @param threadState JDK 线程状态，尚未绑定线程时为 null
 * @param submittedNanos 提交时间，基于 System.nanoTime
 * @param startedNanos 业务开始时间，尚未开始时为 0
 * @param runningDurationNanos 已运行或已等待时长
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:50
 */
public record VirtualTaskSnapshot(long taskId,
                                  String group,
                                  VirtualTaskState taskState,
                                  long threadId,
                                  String threadName,
                                  boolean virtual,
                                  Thread.State threadState,
                                  long submittedNanos,
                                  long startedNanos,
                                  long runningDurationNanos) {
}
