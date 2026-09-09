package com.peach.virtualthread.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单个业务组的活动任务注册表。
 *
 * <p>只保存已经通过 Admission 的活动任务，任务 runner 完成资源清理后立即移除。由于 Admission
 * 总容量受 maxConcurrency + maxPending 限制，因此该注册表天然有界。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:50
 */
public final class TaskRegistry {

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<Long, TaskControl> tasks = new ConcurrentHashMap<>();

    /**
     * 获取下一个组内任务 ID。
     *
     * @return 单调递增任务 ID
     */
    public long nextTaskId() {
        return sequence.incrementAndGet();
    }

    /**
     * 注册活动任务。
     *
     * @param control 任务控制对象
     */
    public void register(TaskControl control) {
        TaskControl previous = tasks.putIfAbsent(control.taskId(), control);
        if (previous != null) {
            throw new IllegalStateException("Duplicate virtual task id: " + control.taskId());
        }
    }

    /**
     * 移除已完成资源清理的任务。
     *
     * @param control 任务控制对象
     */
    public void remove(TaskControl control) {
        tasks.remove(control.taskId(), control);
    }

    /**
     * 获取活动任务快照。
     *
     * @return 按 taskId 排序的安全快照列表
     */
    public List<VirtualTaskSnapshot> snapshots() {
        List<VirtualTaskSnapshot> result = new ArrayList<>(tasks.size());
        for (TaskControl control : tasks.values()) {
            result.add(control.snapshot());
        }
        result.sort(Comparator.comparingLong(VirtualTaskSnapshot::taskId));
        return List.copyOf(result);
    }

    /**
     * 获取活动任务数量。
     *
     * @return 当前注册数量
     */
    public int size() {
        return tasks.size();
    }

    /**
     * 获取当前活动任务控制对象副本，用于 shutdownNow 中断。
     *
     * @return 活动任务控制对象列表
     */
    public List<TaskControl> controls() {
        return List.copyOf(tasks.values());
    }
}
