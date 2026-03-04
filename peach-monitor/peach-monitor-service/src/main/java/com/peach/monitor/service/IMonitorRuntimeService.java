package com.peach.monitor.service;

import com.peach.monitor.entity.monitor.MonitorSnapshotDTO;

public interface IMonitorRuntimeService {

    MonitorSnapshotDTO snapshot();
}
