package com.peach.redission.core;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 隔离区域选择器
 * 用于在多个隔离区域（分区）之间轮询选择，以实现负载均衡
 */
public class IsolationRegionSelector {

	/**
	 * 计数器，用于轮询选择隔离区域
	 */
	private final AtomicInteger count = new AtomicInteger(0);

	/**
	 * 阈值，当计数器达到此值时会重置
	 */
	private final int thresholdValue;


	public IsolationRegionSelector(int thresholdValue) {
		this.thresholdValue = thresholdValue;
	}
	
	/**
	 * 获取下一个隔离区域索引
	 * 使用原子操作而非同步方法来提高并发性能
	 * 
	 * @return 隔离区域索引
	 */
	public int getIndex() {
		int cur = count.getAndUpdate(current -> {
			if (current >= thresholdValue - 1) {
				return 0; // 重置为0而不是1，更符合索引习惯
			} else {
				return current + 1;
			}
		});
		return cur;
	}
}