package com.peach.virtualthread.state;

/**
 * 虚拟线程业务组运行快照。
 *
 * <p>所有字段均为观测数据，不参与 Admission 或 Execution 并发判定。</p>
 *
 * @param group 业务组
 * @param executorState 执行器状态
 * @param maxConcurrency 最大执行并发
 * @param maxPending 最大 Pending 数量
 * @param running 当前运行任务数
 * @param pending 当前 Pending 任务数
 * @param inFlight 当前已准入活动任务数
 * @param submitted 累计已接受任务数
 * @param completed 累计成功任务数
 * @param failed 累计失败任务数
 * @param cancelled 累计取消任务数
 * @param rejected 累计拒绝任务数
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
public record VirtualGroupSnapshot(String group,
                                   VirtualExecutorState executorState,
                                   int maxConcurrency,
                                   int maxPending,
                                   int running,
                                   int pending,
                                   int inFlight,
                                   long submitted,
                                   long completed,
                                   long failed,
                                   long cancelled,
                                   long rejected) {
}
