package com.peach.fileservice.proxy;


import org.springframework.stereotype.Indexed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ClamAV 客户端工厂
 *
 * <p>负责创建 ClamAV 病毒扫描客户端实例。
 * 通过配置注入 ClamAV 服务连接参数，提供统一的客户端创建入口。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Indexed
@Component
public class ClamAVClientFactory {

    /**
     * ClamAV 服务主机名
     */
    @Value("${clamd.host}")
    private String hostname;

    /**
     * ClamAV 服务端口
     */
    @Value("${clamd.port}")
    private int port;

    /**
     * 连接超时时间（毫秒）
     */
    @Value("${clamd.timeout}")
    private int timeout;

    /**
     * 创建新的 ClamAV 客户端实例
     *
     * @return ClamAV 客户端实例
     */
    public ClamAVClient newClient() {
        return new ClamAVClient(hostname, port, timeout);
    }
}
