package com.peach.monitor.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Indexed;
import com.peach.monitor.entity.monitor.MonitorSnapshotDTO;
import com.peach.monitor.service.IMonitorRuntimeService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@Indexed
@Service
@RequiredArgsConstructor
public class MonitorRuntimeServiceImpl implements IMonitorRuntimeService {

    private static final String JSON_KEY_ADDRESS = "address";

    private static final String JSON_KEY_MESSAGE = "message";

    private static final String STATUS_NOT_CONFIGURED = "not_configured";

    private static final String JSON_KEY_STATUS = "status";

    private static final String JSON_KEY_ERROR = "error";


        private final Environment environment;

        private final ObjectProvider<DataSource> dataSourceProvider;

        private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;

    @Override
    public MonitorSnapshotDTO snapshot() {
        MonitorSnapshotDTO monitorSnapshot = new MonitorSnapshotDTO();
        monitorSnapshot.setServiceName(environment.getProperty("spring.application.name", "peach-monitor"));
        monitorSnapshot.setTimestamp(System.currentTimeMillis());

        monitorSnapshot.setHostInfo(buildHostInfo());
        monitorSnapshot.setJvmInfo(buildJvmInfo());
        monitorSnapshot.setDatabaseInfo(buildDatabaseInfo());
        monitorSnapshot.setMiddlewareInfo(buildMiddlewareInfo());
        return monitorSnapshot;
    }

    private Map<String, Object> buildHostInfo() {
        Map<String, Object> hostInfo = new LinkedHashMap<String, Object>();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            hostInfo.put("hostName", localHost.getHostName());
            hostInfo.put("hostAddress", localHost.getHostAddress());
        } catch (Exception e) {
            hostInfo.put("hostName", "unknown");
            hostInfo.put("hostAddress", "unknown");
            hostInfo.put(JSON_KEY_ERROR, e.getMessage());
        }

        hostInfo.put("osName", System.getProperty("os.name"));
        hostInfo.put("osArch", System.getProperty("os.arch"));
        hostInfo.put("osVersion", System.getProperty("os.version"));
        hostInfo.put("processors", operatingSystemMXBean.getAvailableProcessors());
        hostInfo.put("systemLoadAverage", operatingSystemMXBean.getSystemLoadAverage());
        return hostInfo;
    }

    private Map<String, Object> buildJvmInfo() {
        Map<String, Object> jvmInfo = new LinkedHashMap<String, Object>();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        Runtime runtime = Runtime.getRuntime();

        jvmInfo.put("javaVersion", System.getProperty("java.version"));
        jvmInfo.put("uptimeMs", runtimeMXBean.getUptime());
        jvmInfo.put("startTime", runtimeMXBean.getStartTime());
        jvmInfo.put("threadCount", threadMXBean.getThreadCount());
        jvmInfo.put("heapUsedBytes", heapMemoryUsage.getUsed());
        jvmInfo.put("heapMaxBytes", heapMemoryUsage.getMax());
        jvmInfo.put("nonHeapUsedBytes", nonHeapMemoryUsage.getUsed());
        jvmInfo.put("jvmFreeMemoryBytes", runtime.freeMemory());
        jvmInfo.put("jvmTotalMemoryBytes", runtime.totalMemory());
        jvmInfo.put("jvmMaxMemoryBytes", runtime.maxMemory());
        return jvmInfo;
    }

    private Map<String, Object> buildDatabaseInfo() {
        Map<String, Object> databaseInfo = new LinkedHashMap<String, Object>();
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            databaseInfo.put(JSON_KEY_STATUS, STATUS_NOT_CONFIGURED);
            databaseInfo.put(JSON_KEY_MESSAGE, "当前服务未注入 DataSource");
            return databaseInfo;
        }

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();

            databaseInfo.put(JSON_KEY_STATUS, "up");
            databaseInfo.put("productName", metaData.getDatabaseProductName());
            databaseInfo.put("productVersion", metaData.getDatabaseProductVersion());
            databaseInfo.put("driverName", metaData.getDriverName());
            databaseInfo.put("url", metaData.getURL());
            databaseInfo.put("username", metaData.getUserName());
            databaseInfo.put("active", validateConnection(connection));
        } catch (Exception e) {
            databaseInfo.put(JSON_KEY_STATUS, "down");
            databaseInfo.put(JSON_KEY_ERROR, e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
        return databaseInfo;
    }

    private Boolean validateConnection(Connection connection) {
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT 1");
            return resultSet.next();
        } catch (Exception e) {
            return Boolean.FALSE;
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
    }

    private Map<String, Object> buildMiddlewareInfo() {
        Map<String, Object> middlewareInfo = new LinkedHashMap<String, Object>();
        middlewareInfo.put("redis", buildRedisInfo());
        middlewareInfo.put("nacos", buildNacosInfo());
        return middlewareInfo;
    }

    private Map<String, Object> buildRedisInfo() {
        Map<String, Object> redisInfo = new LinkedHashMap<String, Object>();
        StringRedisTemplate stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        if (stringRedisTemplate == null) {
            redisInfo.put(JSON_KEY_STATUS, STATUS_NOT_CONFIGURED);
            redisInfo.put(JSON_KEY_MESSAGE, "当前服务未注入 StringRedisTemplate");
            return redisInfo;
        }

        RedisConnection redisConnection = null;
        try {
            var connectionFactory = stringRedisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                redisInfo.put(JSON_KEY_STATUS, STATUS_NOT_CONFIGURED);
                redisInfo.put(JSON_KEY_MESSAGE, "StringRedisTemplate has no connection factory");
                return redisInfo;
            }
            redisConnection = connectionFactory.getConnection();
            String pingResult = redisConnection.ping();
            redisInfo.put(JSON_KEY_STATUS, "up");
            redisInfo.put("ping", pingResult);
        } catch (Exception e) {
            redisInfo.put(JSON_KEY_STATUS, "down");
            redisInfo.put(JSON_KEY_ERROR, e.getMessage());
        } finally {
            if (redisConnection != null) {
                try {
                    redisConnection.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
        return redisInfo;
    }

    private Map<String, Object> buildNacosInfo() {
        Map<String, Object> nacosInfo = new LinkedHashMap<String, Object>();
        String serverAddr = firstProperty(
                "spring.cloud.nacos.discovery.server-addr",
                "spring.cloud.nacos.config.server-addr");
        if (!org.springframework.util.StringUtils.hasText(serverAddr)) {
            nacosInfo.put(JSON_KEY_STATUS, STATUS_NOT_CONFIGURED);
            nacosInfo.put(JSON_KEY_MESSAGE, "未配置 nacos server-addr");
            return nacosInfo;
        }

        String[] hostAndPort = serverAddr.split(",")[0].split(":");
        String host = hostAndPort[0].trim();
        int port = 8848;
        if (hostAndPort.length > 1) {
            try {
                port = Integer.parseInt(hostAndPort[1].trim());
            } catch (Exception e) {
                nacosInfo.put(JSON_KEY_STATUS, "down");
                nacosInfo.put(JSON_KEY_ADDRESS, serverAddr);
                nacosInfo.put(JSON_KEY_ERROR, "nacos端口配置非法: " + e.getMessage());
                return nacosInfo;
            }
        }

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 1500);
            nacosInfo.put(JSON_KEY_STATUS, "up");
            nacosInfo.put(JSON_KEY_ADDRESS, host + ":" + port);
        } catch (Exception e) {
            nacosInfo.put(JSON_KEY_STATUS, "down");
            nacosInfo.put(JSON_KEY_ADDRESS, host + ":" + port);
            nacosInfo.put(JSON_KEY_ERROR, e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
        return nacosInfo;
    }

    @Nullable
    private String firstProperty(String primaryKey, String fallbackKey) {
        String value = environment.getProperty(primaryKey);
        return value != null ? value : environment.getProperty(fallbackKey);
    }
}
