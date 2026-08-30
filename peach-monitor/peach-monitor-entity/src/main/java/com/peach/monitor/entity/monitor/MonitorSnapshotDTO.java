package com.peach.monitor.entity.monitor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监控Snapshot传输对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

@Schema(description = "监控快照传输对象")
public class MonitorSnapshotDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "服务名称")
    private String serviceName;

    @Schema(description = "快照时间戳")
    private long timestamp;

    @Schema(description = "主机监控信息")
    private Map<String, Object> hostInfo = new LinkedHashMap<>();

    @Schema(description = "JVM监控信息")
    private Map<String, Object> jvmInfo = new LinkedHashMap<>();

    @Schema(description = "数据库监控信息")
    private Map<String, Object> databaseInfo = new LinkedHashMap<>();

    @Schema(description = "中间件监控信息")
    private Map<String, Object> middlewareInfo = new LinkedHashMap<>();

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
