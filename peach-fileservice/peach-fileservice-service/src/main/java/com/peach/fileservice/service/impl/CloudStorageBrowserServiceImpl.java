package com.peach.fileservice.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import com.peach.config.StorageProperties;
import com.peach.content.UploadContent;
import com.peach.fileservice.dao.CloudStorageInstanceDao;
import com.peach.fileservice.dto.CloudStorageListDTO;
import com.peach.fileservice.service.ICloudStorageBrowserService;
import com.peach.fileservice.vo.CloudStorageInstanceVO;
import com.peach.fileservice.vo.CloudStorageObjectNodeVO;
import com.peach.fileservice.vo.CloudStorageObjectPageVO;
import com.peach.fileservice.vo.CloudStorageUploadVO;
import com.peach.manager.CloudStorageManagerService;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.response.UploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Default browser service for persisted storage instances.
 */
@Indexed
@Service
@RequiredArgsConstructor
public class CloudStorageBrowserServiceImpl implements ICloudStorageBrowserService {

        private final CloudStorageInstanceDao cloudStorageInstanceDao;

        private final CloudStorageInstanceSupport cloudStorageInstanceSupport;

        private final CloudStorageManagerService cloudStorageManagerService;

    @Override
    public boolean bucketExists(String instanceId) {
        return cloudStorageManagerService.bucketExists(providerConfig(instanceId));
    }

    @Override
    public boolean objectExists(String instanceId, String objectKey) {
        return cloudStorageManagerService.objectExists(providerConfig(instanceId), objectKey);
    }

    @Override
    public CloudStorageObjectPageVO list(String instanceId, CloudStorageListDTO data) {
        CloudStorageInstanceVO instanceVO = requireEnabledInstance(instanceId);
        StorageProperties.StorageProvider providerConfig = cloudStorageInstanceSupport.toProviderConfig(toInstanceDO(instanceVO));
        ListObjectsRequest.Builder builder = ListObjectsRequest.builder()
                .maxKeys(data == null || data.getMaxKeys() == null ? 200 : data.getMaxKeys())
                .recursive(data != null && Boolean.TRUE.equals(data.getRecursive()));
        if (data != null && StringUtil.isNotBlank(data.getPath())) {
            builder.prefix(data.getPath());
        }
        if (data != null && StringUtil.isNotBlank(data.getContinuationToken())) {
            builder.continuationToken(data.getContinuationToken());
        }
        ListObjectsResult result = cloudStorageManagerService.list(providerConfig, builder.build());
        CloudStorageObjectPageVO pageVO = new CloudStorageObjectPageVO();
        pageVO.setInstanceId(instanceId);
        pageVO.setBucketName(instanceVO.getBucketName());
        pageVO.setPrefix(instanceVO.getPrefix());
        pageVO.setPath(data == null ? null : data.getPath());
        pageVO.setTruncated(result.isTruncated());
        pageVO.setNextContinuationToken(result.getNextContinuationToken());
        pageVO.setCommonPrefixes(result.getCommonPrefixes());
        List<CloudStorageObjectNodeVO> items = new ArrayList<CloudStorageObjectNodeVO>();
        for (ObjectInfo item : result.getItems()) {
            items.add(toNode(item, Boolean.FALSE));
        }
        for (String commonPrefix : result.getCommonPrefixes()) {
            items.add(toDirectoryNode(commonPrefix));
        }
        pageVO.setItems(items);
        return pageVO;
    }

    @Override
    public CloudStorageObjectNodeVO stat(String instanceId, String objectKey) {
        StorageProperties.StorageProvider providerConfig = providerConfig(instanceId);
        ObjectInfo objectInfo = cloudStorageManagerService.stat(providerConfig,
                DownloadObjectRequest.builder().objectKey(objectKey).build());
        return toNode(objectInfo, Boolean.FALSE);
    }

    @Override
    public CloudStorageUploadVO upload(String instanceId, String targetPath, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("upload file is empty");
        }
        StorageProperties.StorageProvider providerConfig = providerConfig(instanceId);
        String objectKey = buildTargetObjectKey(targetPath, file.getOriginalFilename());
        try {
            UploadObjectRequest request = UploadObjectRequest.builder()
                    .objectKey(objectKey)
                    .content(UploadContent.of(file.getInputStream(), file.getSize()))
                    .contentType(file.getContentType())
                    .build();
            UploadResult result = cloudStorageManagerService.upload(providerConfig, request);
            CloudStorageUploadVO uploadVO = new CloudStorageUploadVO();
            uploadVO.setProviderName(result.getProviderName());
            uploadVO.setBucketName(result.getBucketName());
            uploadVO.setObjectKey(result.getObjectKey());
            uploadVO.setSize(result.getSize());
            uploadVO.setUrl(result.getUrl());
            return uploadVO;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload file", ex);
        }
    }

    @Override
    public void createDirectory(String instanceId, String path) {
        cloudStorageManagerService.createDirectory(providerConfig(instanceId), path);
    }

    @Override
    public void deleteObject(String instanceId, String objectKey) {
        cloudStorageManagerService.deleteObject(providerConfig(instanceId),
                DeleteObjectRequest.builder().objectKey(objectKey).build());
    }

    @Override
    public void deleteDirectory(String instanceId, String path) {
        cloudStorageManagerService.deleteDirectory(providerConfig(instanceId), path);
    }

    private StorageProperties.StorageProvider providerConfig(String instanceId) {
        CloudStorageInstanceVO instanceVO = requireEnabledInstance(instanceId);
        return cloudStorageInstanceSupport.toProviderConfig(toInstanceDO(instanceVO));
    }

    private CloudStorageInstanceVO requireEnabledInstance(String instanceId) {
        CloudStorageInstanceVO instanceVO = cloudStorageInstanceDao.selectById(instanceId);
        if (instanceVO == null) {
            throw new RuntimeException("cloud storage instance not found");
        }
        if (!Integer.valueOf(1).equals(instanceVO.getEnabled())) {
            throw new RuntimeException("cloud storage instance is disabled");
        }
        return instanceVO;
    }

    private CloudStorageObjectNodeVO toNode(ObjectInfo item, Boolean directory) {
        CloudStorageObjectNodeVO nodeVO = new CloudStorageObjectNodeVO();
        nodeVO.setDirectory(directory);
        nodeVO.setObjectKey(item.getObjectKey());
        nodeVO.setPath(item.getObjectKey());
        nodeVO.setName(extractName(item.getObjectKey()));
        nodeVO.setSize(item.getSize());
        nodeVO.setEtag(item.getEtag());
        nodeVO.setContentType(item.getContentType());
        nodeVO.setLastModified(item.getLastModified() == null ? null
                : DateUtil.formatLocalDateTime(item.getLastModified().atZone(ZoneId.systemDefault()).toLocalDateTime()));
        return nodeVO;
    }

    private CloudStorageObjectNodeVO toDirectoryNode(String commonPrefix) {
        CloudStorageObjectNodeVO nodeVO = new CloudStorageObjectNodeVO();
        nodeVO.setDirectory(Boolean.TRUE);
        nodeVO.setPath(commonPrefix);
        nodeVO.setObjectKey(commonPrefix);
        nodeVO.setName(extractName(commonPrefix));
        nodeVO.setSize(0L);
        return nodeVO;
    }

    private String buildTargetObjectKey(String targetPath, String originalFilename) {
        if (StringUtil.isBlank(originalFilename)) {
            throw new RuntimeException("original file name is empty");
        }
        if (StringUtil.isBlank(targetPath)) {
            return originalFilename;
        }
        return targetPath + "/" + originalFilename;
    }

    private String extractName(String path) {
        if (StringUtil.isBlank(path)) {
            return path;
        }
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }

    private com.peach.fileservice.entity.CloudStorageInstanceDO toInstanceDO(CloudStorageInstanceVO instanceVO) {
        com.peach.fileservice.entity.CloudStorageInstanceDO instanceDO = new com.peach.fileservice.entity.CloudStorageInstanceDO();
        instanceDO.setInstanceId(instanceVO.getInstanceId());
        instanceDO.setInstanceName(instanceVO.getInstanceName());
        instanceDO.setStoreType(instanceVO.getStoreType());
        instanceDO.setEndpoint(instanceVO.getEndpoint());
        instanceDO.setRegion(instanceVO.getRegion());
        instanceDO.setBucketName(instanceVO.getBucketName());
        instanceDO.setPrefix(instanceVO.getPrefix());
        instanceDO.setAccessKey(instanceVO.getAccessKey());
        instanceDO.setSecretKey(instanceVO.getSecretKey());
        instanceDO.setRootPath(instanceVO.getRootPath());
        instanceDO.setDomain(instanceVO.getDomain());
        instanceDO.setPathStyleAccess(instanceVO.getPathStyleAccess());
        instanceDO.setPublicRead(instanceVO.getPublicRead());
        instanceDO.setExtraJson(instanceVO.getExtraJson());
        instanceDO.setEnabled(instanceVO.getEnabled());
        instanceDO.setRemark(instanceVO.getRemark());
        return instanceDO;
    }
}
