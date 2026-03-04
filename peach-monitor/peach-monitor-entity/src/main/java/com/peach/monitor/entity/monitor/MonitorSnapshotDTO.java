package com.peach.monitor.entity.monitor;

import java.util.LinkedHashMap;
import java.util.Map;

public class MonitorSnapshotDTO {

    private String serviceName;
    private long timestamp;
    private Map<String, Object> hostInfo = new LinkedHashMap<String, Object>();
    private Map<String, Object> jvmInfo = new LinkedHashMap<String, Object>();
    private Map<String, Object> databaseInfo = new LinkedHashMap<String, Object>();
    private Map<String, Object> middlewareInfo = new LinkedHashMap<String, Object>();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getHostInfo() {
        return hostInfo;
    }

    public void setHostInfo(Map<String, Object> hostInfo) {
        this.hostInfo = hostInfo;
    }

    public Map<String, Object> getJvmInfo() {
        return jvmInfo;
    }

    public void setJvmInfo(Map<String, Object> jvmInfo) {
        this.jvmInfo = jvmInfo;
    }

    public Map<String, Object> getDatabaseInfo() {
        return databaseInfo;
    }

    public void setDatabaseInfo(Map<String, Object> databaseInfo) {
        this.databaseInfo = databaseInfo;
    }

    public Map<String, Object> getMiddlewareInfo() {
        return middlewareInfo;
    }

    public void setMiddlewareInfo(Map<String, Object> middlewareInfo) {
        this.middlewareInfo = middlewareInfo;
    }
}
