package com.peach.fileservice.service.impl.security;

import com.peach.fileservice.proxy.ClamAVClient;
import com.peach.fileservice.proxy.ClamAVClientFactory;
import com.peach.fileservice.service.IFileStoreSecurityStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 11:04
 */
@Slf4j
@Indexed
@Component
public class ClamavFileStoreSecurityStrategy implements IFileStoreSecurityStrategy {

    @Resource
    private ClamAVClientFactory clamAVClientFactory;

    @Override
    public boolean scanStream(InputStream inputStream) {
        ClamAVClient clamAVClient = clamAVClientFactory.newClient();
        try {
            byte[] bytes = clamAVClient.scan(inputStream);
            boolean cleanReply = ClamAVClient.isCleanReply(bytes);
            return cleanReply;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
