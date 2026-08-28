package com.peach.redission.delayqueue.core;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * 隔离区域选择器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 隔离区域选择器
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
		return count.getAndUpdate(current -> current >= thresholdValue - 1 ? 0 : current + 1);
	}
}