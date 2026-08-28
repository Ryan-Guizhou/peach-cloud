package com.peach.monitor.service;

import com.peach.monitor.entity.monitor.MonitorSnapshotDTO;

/**
 * IMonitorRuntime服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

public interface IMonitorRuntimeService {

    MonitorSnapshotDTO snapshot();
}
