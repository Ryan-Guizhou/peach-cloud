package com.peach.fileservice.service.impl;

import com.peach.common.util.StringUtil;
import com.peach.config.StorageProperties;
import com.peach.enums.StorageType;
import com.peach.fileservice.dao.CloudStorageInstanceDao;
import com.peach.fileservice.dto.CloudStorageListDTO;
import com.peach.fileservice.service.ICloudStorageBrowserService;
import com.peach.fileservice.vo.CloudStorageInstanceVO;
import com.peach.fileservice.vo.CloudStorageObjectNodeVO;
import com.peach.fileservice.vo.CloudStorageObjectPageVO;
import com.peach.fileservice.vo.CloudStorageUploadVO;
import com.peach.manager.CloudStorageManagerService;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 云存储浏览服务实现。
 *
 * <p>负责云存储对象浏览、上传以及目录和对象删除等操作。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Slf4j
@Indexed
@Service
public class CloudStorageBrowserServiceImpl implements ICloudStorageBrowserService {

    @Resource
    private CloudStorageInstanceDao cloudStorageInstanceDao;

    @Resource
    private CloudStorageManagerService cloudStorageManagerService;

    @Override
    public boolean bucketExists(String instanceId) {
        StorageProperties.StorageProvider providerConfig = buildProviderConfig(requireInstance(instanceId));
        return cloudStorageManagerService.bucketExists(providerConfig);
    }

    @Override
    public boolean objectExists(String instanceId, String objectKey) {
        StorageProperties.StorageProvider providerConfig = buildProviderConfig(requireInstance(instanceId));
        return cloudStorageManagerService.objectExists(providerConfig, objectKey);
    }

    @Override
    public CloudStorageObjectPageVO list(String instanceId, CloudStorageListDTO data) {
        CloudStorageInstanceVO instance = requireInstance(instanceId);
        StorageProperties.StorageProvider providerConfig = buildProviderConfig(instance);
        ListObjectsResult result = cloudStorageManagerService.list(providerConfig, buildListRequest(data));
        return toPageVO(result, data);
    }

    @Override
    public CloudStorageObjectNodeVO stat(String instanceId, String objectKey) {
        StorageProperties.StorageProvider providerConfig = buildProviderConfig(requireInstance(instanceId));
        ObjectInfo objectInfo = cloudStorageManagerService.stat(providerConfig, buildDownloadRequest(objectKey));
        return toNodeVO(objectInfo, false);
    }

    @Override
    public CloudStorageUploadVO upload(String instanceId, String targetPath, MultipartFile file) {
        CloudStorageInstanceVO instance = requireInstance(instanceId);
        StorageProperties.StorageProvider providerConfig = buildProviderConfig(instance);
        String objectKey = buildObjectKey(targetPath, file);
        try {
            com.peach.response.UploadResult result = cloudStorageManagerService.upload(providerConfig,
                    com.peach.request.UploadObjectRequest.builder()
                            .bucketName(providerConfig.getBucketName())
                            .objectKey(objectKey)
                            .content(com.peach.content.UploadContent.of(file.getInputStream(), file.getSize()))
                            .contentType(file.getContentType())
                            .build());
            return toUploadVO(instanceId, result);
        } catch (Exception ex) {
            throw new RuntimeException("?????????", ex);
        }
    }

    @Override
    public void createDirectory(String instanceId, String path) {
        StorageProperties.StorageProvider providerConfig = buildProviderConfig(requireInstance(instanceId));
        cloudStorageManagerService.createDirectory(providerConfig, path);
    }

    @Override
    public void deleteObject(String instanceId, String objectKey) {
        StorageProperties.StorageProvider providerConfig = buildProviderConfig(requireInstance(instanceId));
        cloudStorageManagerService.deleteObject(providerConfig, buildDeleteRequest(objectKey));
    }

    @Override
    public void deleteDirectory(String instanceId, String path) {
        StorageProperties.StorageProvider providerConfig = buildProviderConfig(requireInstance(instanceId));
        cloudStorageManagerService.deleteDirectory(providerConfig, path);
    }

    private CloudStorageInstanceVO requireInstance(String instanceId) {
        if (StringUtil.isBlank(instanceId)) {
            throw new RuntimeException("??ID????");
        }
        CloudStorageInstanceVO instance = cloudStorageInstanceDao.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("????????:" + instanceId);
        }
        return instance;
    }

    private StorageProperties.StorageProvider buildProviderConfig(CloudStorageInstanceVO instance) {
        StorageProperties.StorageProvider provider = new StorageProperties.StorageProvider();
        BeanUtils.copyProperties(instance, provider);
        provider.setName(instance.getInstanceName());
        provider.setType(StorageType.parse(instance.getStoreType()));
        return provider;
    }

    private com.peach.request.ListObjectsRequest buildListRequest(CloudStorageListDTO data) {
        CloudStorageListDTO query = data == null ? new CloudStorageListDTO() : data;
        return com.peach.request.ListObjectsRequest.builder()
                .prefix(query.getPath())
                .recursive(query.getRecursive() == null || query.getRecursive())
                .build();
    }

    private com.peach.request.DownloadObjectRequest buildDownloadRequest(String objectKey) {
        return com.peach.request.DownloadObjectRequest.builder()
                .objectKey(objectKey)
                .build();
    }

    private com.peach.request.DeleteObjectRequest buildDeleteRequest(String objectKey) {
        return com.peach.request.DeleteObjectRequest.builder()
                .objectKey(objectKey)
                .build();
    }

    private CloudStorageObjectPageVO toPageVO(ListObjectsResult result, CloudStorageListDTO data) {
        CloudStorageObjectPageVO vo = new CloudStorageObjectPageVO();
        vo.setPath(result == null ? null : result.getPrefix());
        vo.setTruncated(result != null && result.isTruncated());
        if (result == null) {
            return vo;
        }
        boolean includeFiles = data == null || data.getIncludeFiles() == null || data.getIncludeFiles();
        boolean includeDirectories = data == null || data.getIncludeDirectories() == null || data.getIncludeDirectories();
        Map<String, CloudStorageObjectNodeVO> nodes = new LinkedHashMap<String, CloudStorageObjectNodeVO>();
        if (includeFiles) {
            for (ObjectInfo item : result.getItems()) {
                if (item == null || StringUtil.isBlank(item.getObjectKey())) {
                    continue;
                }
                CloudStorageObjectNodeVO node = toNodeVO(item, false);
                nodes.put(node.getPath(), node);
            }
        }
        if (includeDirectories) {
            for (String prefix : result.getCommonPrefixes()) {
                if (StringUtil.isBlank(prefix)) {
                    continue;
                }
                CloudStorageObjectNodeVO node = toDirectoryNode(prefix, result.getProviderName(), result.getBucketName());
                nodes.put(node.getPath(), node);
            }
        }
        vo.setItems(new ArrayList<CloudStorageObjectNodeVO>(nodes.values()));
        return vo;
    }

    private CloudStorageObjectNodeVO toNodeVO(ObjectInfo item, boolean directory) {
        CloudStorageObjectNodeVO vo = new CloudStorageObjectNodeVO();
        vo.setPath(item.getObjectKey());
        vo.setName(resolveName(item.getObjectKey()));
        vo.setDirectory(directory);
        vo.setSize(item.getSize());
        vo.setContentType(item.getContentType());
        vo.setLastModified(item.getLastModified() == null ? null : item.getLastModified().toString());
        vo.setStorageProvider(item.getProviderName());
        vo.setBucketName(item.getBucketName());
        vo.setObjectKey(item.getObjectKey());
        return vo;
    }

    private CloudStorageObjectNodeVO toDirectoryNode(String path, String providerName, String bucketName) {
        CloudStorageObjectNodeVO vo = new CloudStorageObjectNodeVO();
        vo.setPath(path);
        vo.setName(resolveName(path));
        vo.setDirectory(Boolean.TRUE);
        vo.setSize(0L);
        vo.setStorageProvider(providerName);
        vo.setBucketName(bucketName);
        vo.setObjectKey(path);
        return vo;
    }

    private CloudStorageUploadVO toUploadVO(String instanceId, com.peach.response.UploadResult result) {
        CloudStorageUploadVO vo = new CloudStorageUploadVO();
        vo.setInstanceId(instanceId);
        if (result != null) {
            vo.setStorageProvider(result.getProviderName());
            vo.setBucketName(result.getBucketName());
            vo.setObjectKey(result.getObjectKey());
            vo.setUrl(result.getUrl());
            vo.setEtag(result.getEtag());
        }
        return vo;
    }

    private String buildObjectKey(String targetPath, MultipartFile file) {
        String fileName = file == null ? null : file.getOriginalFilename();
        if (StringUtil.isBlank(fileName)) {
            throw new RuntimeException("???????");
        }
        if (StringUtil.isBlank(targetPath)) {
            return fileName;
        }
        return com.peach.util.StoragePathUtil.joinObjectKey(targetPath, fileName);
    }

    private String resolveName(String path) {
        if (StringUtil.isBlank(path)) {
            return path;
        }
        String normalized = path;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }
}
