package com.peach.fileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.StoreConstants;
import com.peach.fileservice.config.store.LocalProperties;
import com.peach.fileservice.service.AbstractFileStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 10:40
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(
        prefix = StoreConstants.CONDITIONAL_PREFIX,
        name = StoreConstants.CONDITOPNAL_NAME,
        havingValue = StoreConstants.LOCAL)
@EnableConfigurationProperties(LocalProperties.class)
public class LocalFileStoreServiceImpl extends AbstractFileStoreService {

    protected final String rootPath;
    protected final String prefix;
    protected final String proxyHost;
    protected final boolean isEnableClamav;

    public LocalFileStoreServiceImpl(LocalProperties properties) {
        this.rootPath = normalizePath(properties.getRootPath());
        this.prefix = properties.getPrefix();
        this.proxyHost = properties.getProxyHost();
        this.isEnableClamav = properties.isEnableClamav();
        log.info("LocalFileStoreServiceImpl init, rootPath: {}", rootPath);
    }

    @Override
    public boolean copyDir(String sourceDir, String targetDir) {
        try {
            File src = new File(rootPath, normalizePath(sourceDir));
            File dest = new File(rootPath, normalizePath(targetDir));
            FileUtil.copyContent(src, dest, true);
            return true;
        } catch (Exception e) {
            log.error("LocalFileStoreServiceImpl copyDir error", e);
            return false;
        }
    }

    @Override
    public boolean downDir(String sourceDir, String localDir) {
        try {
            File src = new File(rootPath, normalizePath(sourceDir));
            File dest = new File(normalizePath(localDir));
            FileUtil.copyContent(src, dest, true);
            return true;
        } catch (Exception e) {
            log.error("LocalFileStoreServiceImpl downDir error", e);
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String targetPath, String fileName) {
        return uploadInputStream(inputStream, targetPath, fileName);
    }

    @Override
    public String upload(String content, String targetPath, String fileName) {
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            return uploadInputStream(inputStream, targetPath, fileName);
        } catch (Exception e) {
            log.error("LocalFileStoreServiceImpl upload content error", e);
            return StringUtil.EMPTY;
        }
    }

    @Override
    public List<String> upload(File[] files, String targetPath) {
        List<String> urls = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                urls.add(upload(file, targetPath, file.getName()));
            }
        }
        return urls;
    }

    @Override
    public String upload(File file, String targetPath, String fileName) {
        return uploadFile(file, targetPath, fileName);
    }

    @Override
    public boolean download(String targetPath, String localPath, String fileName) {
        try {
            File src = new File(rootPath, normalizePath(targetPath));
            File dest = new File(normalizePath(localPath), fileName);
            FileUtil.copy(src, dest, true);
            return true;
        } catch (Exception e) {
            log.error("LocalFileStoreServiceImpl download error", e);
            return false;
        }
    }

    @Override
    public InputStream getInputStream(String targetPath, String fileName) {
        return getInputStreamByKey(buildPathKey(targetPath, fileName));
    }

    @Override
    public InputStream getInputStreamByKey(String key) {
        try {
            File file = new File(rootPath, normalizePath(key));
            if (file.exists()) {
                return new FileInputStream(file);
            }
        } catch (Exception e) {
            log.error("LocalFileStoreServiceImpl getInputStreamByKey error", e);
        }
        return null;
    }

    @Override
    public boolean delete(String key) {
        if (isHasIllegalChar(key)) return false;
        try {
            File file = new File(rootPath, normalizePath(removeUrlHost(key)));
            return FileUtil.del(file);
        } catch (Exception e) {
            log.error("LocalFileStoreServiceImpl delete error", e);
            return false;
        }
    }

    @Override
    public boolean copyFile(String currentPath, String targetPath) {
        try {
            File src = new File(rootPath, normalizePath(currentPath));
            File dest = new File(rootPath, normalizePath(targetPath));
            FileUtil.copy(src, dest, true);
            return true;
        } catch (Exception e) {
            log.error("LocalFileStoreServiceImpl copyFile error", e);
            return false;
        }
    }

    @Override
    public String getUrlByKey(String key) {
        return getOrgUrlByKey(key, true);
    }

    @Override
    public String getPathByKey(String key) {
        return getOrgUrlByKey(key, false);
    }

    @Override
    public void setPublicReadAcl(String path) {
        // Local storage doesn't support cloud-style ACLs, but we could set file permissions
    }

    @Override
    protected String prefix() {
        return prefix;
    }

    @Override
    protected String proxyHost() {
        return proxyHost;
    }

    @Override
    protected boolean isClamavEnable() {
        return isEnableClamav;
    }

    @Override
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName) {
        if (checkForClamav(inputStream)){
            return StringUtil.EMPTY;
        }
        try {
            String key = buildPathKey(targetPath, fileName);
            File dest = new File(rootPath, key);
            FileUtil.writeFromStream(inputStream, dest);
            return replaceUrlHost(key, false);
        } catch (Exception e) {
            log.error("LocalFileStoreServiceImpl uploadInputStream error", e);
            return StringUtil.EMPTY;
        }
    }

    @Override
    protected String getOrgUrlByKey(String key, boolean isUrl) {
        String keyPath = normalizePath(key);
        File file = new File(rootPath, keyPath);
        if (file.exists()) {
            return replaceUrlHost(keyPath, isUrl);
        }
        return StringUtil.EMPTY;
    }
}
