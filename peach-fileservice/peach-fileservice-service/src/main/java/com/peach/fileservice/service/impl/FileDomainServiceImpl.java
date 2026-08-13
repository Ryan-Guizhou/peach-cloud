package com.peach.fileservice.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.PageResult;
import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import com.peach.content.UploadContent;
import com.peach.fileservice.common.FileDomainConstant;
import com.peach.fileservice.config.FileDomainProperties;
import com.peach.fileservice.dao.FileObjectDao;
import com.peach.fileservice.dao.FileRecordDao;
import com.peach.fileservice.dao.FileUploadSessionDao;
import com.peach.fileservice.dto.FileMultipartCompleteDTO;
import com.peach.fileservice.dto.FileMultipartCompletePartDTO;
import com.peach.fileservice.dto.FileMultipartInitDTO;
import com.peach.fileservice.dto.FileMultipartPartUrlDTO;
import com.peach.fileservice.dto.FileExternalUploadDTO;
import com.peach.fileservice.dto.FileUploadCheckDTO;
import com.peach.fileservice.entity.FileObjectDO;
import com.peach.fileservice.entity.FileRecordDO;
import com.peach.fileservice.entity.FileUploadSessionDO;
import com.peach.fileservice.qo.FileQueryQO;
import com.peach.fileservice.service.IFileDomainService;
import com.peach.fileservice.vo.FileDownloadUrlVO;
import com.peach.fileservice.vo.FileDigestVO;
import com.peach.fileservice.vo.FileExternalFileVO;
import com.peach.fileservice.vo.FileMultipartInitVO;
import com.peach.fileservice.vo.FileMultipartPartVO;
import com.peach.fileservice.vo.FileObjectVO;
import com.peach.fileservice.vo.FileRecordVO;
import com.peach.fileservice.vo.FileUploadCheckVO;
import com.peach.fileservice.vo.FileUploadSessionVO;
import com.peach.fileservice.vo.FileUploadVO;
import com.peach.fileservice.common.util.FileDigestUtils;
import com.peach.request.AbortMultipartUploadRequest;
import com.peach.request.CompleteMultipartUploadRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.InitiateMultipartUploadRequest;
import com.peach.request.PresignedUrlRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.request.UploadPartRequest;
import com.peach.response.CompleteMultipartUploadResult;
import com.peach.response.InitiateMultipartUploadResult;
import com.peach.response.PresignedUrlResult;
import com.peach.response.UploadPartResult;
import com.peach.response.UploadResult;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import com.peach.service.MultiZoneStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;


/**
 * 文件域服务实现类
 *
 * <p>实现文件上传、下载、分片上传、文件管理等核心业务功能。
 * 主要特性包括：</p>
 * <ul>
 *   <li><b>秒传检测</b>：基于 SHA-256 和文件大小检测文件是否已存在，实现秒传</li>
 *   <li><b>分片上传</b>：支持大文件分片上传，提供预签名URL直传</li>
 *   <li><b>逻辑删除</b>：文件删除采用逻辑删除，支持在保留期内恢复</li>
 *   <li><b>定时清理</b>：定期物理删除过期文件和清理未完成会话</li>
 *   <li><b>多存储支持</b>：支持多种云存储服务（MinIO、OSS、COS、OBS等）</li>
 *   <li><b>对象复用</b>：相同内容的文件共享存储对象，节省存储空间</li>
 * </ul>
 *
 * <p>核心业务流程：</p>
 * <ol>
 *   <li>上传前校验（秒传检测）</li>
 *   <li>文件上传（简单上传或分片上传）</li>
 *   <li>创建文件记录和文件对象</li>
 *   <li>文件下载（生成预签名URL）</li>
 *   <li>文件管理（查询、删除、恢复）</li>
 *   <li>定时清理（过期文件和会话）</li>
 * </ol>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 * @see IFileDomainService
 */
@Slf4j
@Indexed
@Service
public class FileDomainServiceImpl implements IFileDomainService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DateUtil.TIME_PATTERN);

    @Resource
    private FileObjectDao fileObjectDao;

    @Resource
    private FileRecordDao fileRecordDao;

    @Resource
    private FileUploadSessionDao fileUploadSessionDao;

    @Resource
    private MultiZoneStorage multiZoneStorage;

    @Resource
    private FileDomainProperties fileDomainProperties;

    @Override
    public FileDigestVO calculateSha256(MultipartFile file) {
        FileDigestVO result = new FileDigestVO();
        result.setAlgorithm(FileDomainConstant.DIGEST_SHA256_ALGORITHM);
        result.setSha256(FileDigestUtils.sha256(file));
        result.setFileSize(file.getSize());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO uploadExternal(FileExternalUploadDTO data, MultipartFile file) {
        if (data == null) {
            throw new IllegalArgumentException("upload data is null");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("upload file is empty");
        }
        FileUploadCheckDTO check = new FileUploadCheckDTO();
        check.setSha256(FileDigestUtils.sha256(file));
        check.setFileSize(file.getSize());
        check.setFileName(resolveFileName(file));
        check.setDisplayName(data.getDisplayName());
        check.setContentType(data.getContentType());
        check.setBizType(data.getBizType());
        check.setBizId(data.getBizId());
        check.setBizTag(data.getBizTag());
        check.setRemark(data.getRemark());
        check.setStorageProvider(data.getStorageProvider());
        return upload(check, file);
    }

    @Override
    public FileExternalFileVO selectExternalByFileId(String fileId) {
        FileRecordVO source = selectByFileId(fileId);
        if (source == null) {
            return null;
        }
        FileExternalFileVO result = new FileExternalFileVO();
        result.setFileId(source.getFileId());
        result.setFileName(source.getFileName());
        result.setDisplayName(source.getDisplayName());
        result.setContentType(source.getContentType());
        result.setFileSize(source.getFileSize());
        result.setFileStatus(source.getFileStatus());
        return result;
    }

    private String resolveFileName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        return StringUtil.isBlank(fileName) ? FileDomainConstant.DEFAULT_FILE_NAME : fileName;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadCheckVO uploadCheck(FileUploadCheckDTO data) {
        validateCheckData(data);
        FileObjectVO objectVO = fileObjectDao.selectActiveBySha256AndSize(data.getSha256(), data.getFileSize());
        FileUploadCheckVO result = new FileUploadCheckVO();
        if (objectVO == null) {
            result.setInstantUpload(Boolean.FALSE);
            result.setObjectReused(Boolean.FALSE);
            return result;
        }
        FileRecordDO recordDO = buildFileRecord(data, objectVO.getObjectId(), data.getFileSize());
        fileRecordDao.insert(recordDO);
        fileObjectDao.increaseRefCount(objectVO.getObjectId(), 1, DateUtil.nowTime(), currentOperator());
        result.setInstantUpload(Boolean.TRUE);
        result.setObjectReused(Boolean.TRUE);
        result.setFileId(recordDO.getFileId());
        return result;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO upload(FileUploadCheckDTO data, MultipartFile file) {
        validateCheckData(data);
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("upload file is empty");
        }
        if (data.getFileSize() != null && file.getSize() != data.getFileSize()) {
            throw new RuntimeException("file size mismatch");
        }
        FileObjectVO objectVO = fileObjectDao.selectActiveBySha256AndSize(data.getSha256(), data.getFileSize());
        if (objectVO != null) {
            return createRecordFromExistingObject(data, objectVO);
        }
        byte[] bytes = readFileBytes(file);
        verifyDigest(data, bytes);
        String objectId = IDGeneratorUtil.UUID();
        String fileId = IDGeneratorUtil.UUID();
        String objectKey = buildObjectKey(data.getBizType(), data.getFileName());
        UploadObjectRequest request = UploadObjectRequest.builder()
                .bucketName(null)
                .objectKey(objectKey)
                .content(UploadContent.of(bytes))
                .contentType(resolveContentType(data.getContentType(), file.getContentType()))
                .build();
        UploadResult uploadResult = uploadObject(resolveProvider(data.getStorageProvider()), request);
        FileObjectDO objectDO = buildFileObject(data, objectId, uploadResult, file.getOriginalFilename());
        FileRecordDO recordDO = buildFileRecord(data, objectId, data.getFileSize());
        recordDO.setFileId(fileId);
        fileObjectDao.insert(objectDO);
        fileRecordDao.insert(recordDO);
        FileUploadVO result = new FileUploadVO();
        result.setFileId(fileId);
        result.setInstantUpload(Boolean.FALSE);
        result.setObjectReused(Boolean.FALSE);
        result.setFileName(recordDO.getFileName());
        result.setFileSize(recordDO.getFileSize());
        return result;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMultipartInitVO initMultipartUpload(FileMultipartInitDTO data) {
        validateCheckData(data);
        FileObjectVO objectVO = fileObjectDao.selectActiveBySha256AndSize(data.getSha256(), data.getFileSize());
        if (objectVO != null) {
            FileUploadVO uploadVO = createRecordFromExistingObject(data, objectVO);
            FileMultipartInitVO result = new FileMultipartInitVO();
            result.setInstantUpload(Boolean.TRUE);
            result.setFileId(uploadVO.getFileId());
            return result;
        }
        String sessionId = IDGeneratorUtil.UUID();
        String fileId = IDGeneratorUtil.UUID();
        String objectId = IDGeneratorUtil.UUID();
        String objectKey = buildObjectKey(data.getBizType(), data.getFileName());
        String providerName = resolveProvider(data.getStorageProvider());
        InitiateMultipartUploadResult initiateResult = initiateMultipart(providerName, objectKey,
                resolveContentType(data.getContentType(), null));

        FileUploadSessionDO sessionDO = new FileUploadSessionDO();
        sessionDO.setSessionId(sessionId);
        sessionDO.setFileId(fileId);
        sessionDO.setObjectId(objectId);
        sessionDO.setHashSha256(data.getSha256());
        sessionDO.setHashMd5(data.getMd5());
        sessionDO.setFileSize(data.getFileSize());
        sessionDO.setFileName(data.getFileName());
        sessionDO.setDisplayName(data.getDisplayName());
        sessionDO.setContentType(resolveContentType(data.getContentType(), null));
        sessionDO.setBizType(data.getBizType());
        sessionDO.setBizId(data.getBizId());
        sessionDO.setBizTag(data.getBizTag());
        sessionDO.setRemark(data.getRemark());
        sessionDO.setStorageProvider(initiateResult.getProviderName());
        sessionDO.setBucketName(initiateResult.getBucketName());
        sessionDO.setObjectKey(initiateResult.getObjectKey());
        sessionDO.setUploadId(initiateResult.getUploadId());
        sessionDO.setSessionStatus(FileDomainConstant.SessionStatus.INITIATED);
        sessionDO.setExpireTime(format(LocalDateTime.now().plusMinutes(fileDomainProperties.getUploadSessionExpireMinutes())));
        sessionDO.setIsDelete(FileDomainConstant.LogicDelete.NO);
        sessionDO.fillCreateTime();
        fileUploadSessionDao.insert(sessionDO);

        FileMultipartInitVO result = new FileMultipartInitVO();
        result.setInstantUpload(Boolean.FALSE);
        result.setFileId(fileId);
        result.setSessionId(sessionId);
        result.setUploadId(initiateResult.getUploadId());
        result.setProviderName(initiateResult.getProviderName());
        result.setBucketName(initiateResult.getBucketName());
        result.setObjectKey(initiateResult.getObjectKey());
        result.setExpiresAt(sessionDO.getExpireTime());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMultipartPartVO prepareMultipartPart(FileMultipartPartUrlDTO data) {
        FileUploadSessionVO sessionVO = requireActiveSession(data.getSessionId());
        UploadPartResult uploadPartResult = preparePart(sessionVO, data.getPartNumber());
        fileUploadSessionDao.updateSessionStatus(sessionVO.getSessionId(), FileDomainConstant.SessionStatus.UPLOADING,
                DateUtil.nowTime(), currentOperator());
        FileMultipartPartVO result = new FileMultipartPartVO();
        result.setSessionId(sessionVO.getSessionId());
        result.setUploadId(sessionVO.getUploadId());
        result.setPartNumber(uploadPartResult.getPartNumber());
        result.setUploadUrl(uploadPartResult.getUrl());
        result.setExpiresAt(format(uploadPartResult.getExpiresAt()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO completeMultipartUpload(FileMultipartCompleteDTO data) {
        FileUploadSessionVO sessionVO = requireSession(data.getSessionId());
        if (FileDomainConstant.SessionStatus.COMPLETED.equals(sessionVO.getSessionStatus())) {
            FileUploadVO result = new FileUploadVO();
            result.setFileId(sessionVO.getFileId());
            result.setInstantUpload(Boolean.FALSE);
            result.setObjectReused(Boolean.FALSE);
            result.setFileName(sessionVO.getFileName());
            result.setFileSize(sessionVO.getFileSize());
            return result;
        }
        CompleteMultipartUploadResult completeResult = completeMultipart(sessionVO, data.getParts());
        verifyUploadedObjectDigest(sessionVO);
        FileObjectVO exists = fileObjectDao.selectActiveBySha256AndSize(sessionVO.getHashSha256(), sessionVO.getFileSize());
        FileUploadVO result;
        if (exists != null) {
            deletePhysicalObject(sessionVO.getStorageProvider(), sessionVO.getBucketName(), sessionVO.getObjectKey());
            result = createRecordFromExistingSession(sessionVO, exists);
        } else {
            FileObjectDO objectDO = new FileObjectDO();
            objectDO.setObjectId(sessionVO.getObjectId());
            objectDO.setHashSha256(sessionVO.getHashSha256());
            objectDO.setHashMd5(sessionVO.getHashMd5());
            objectDO.setFileSize(sessionVO.getFileSize());
            objectDO.setStorageProvider(completeResult.getProviderName());
            objectDO.setBucketName(completeResult.getBucketName());
            objectDO.setObjectKey(completeResult.getObjectKey());
            objectDO.setOriginFileName(sessionVO.getFileName());
            objectDO.setContentType(sessionVO.getContentType());
            objectDO.setExtension(extractExtension(sessionVO.getFileName()));
            objectDO.setStorageStatus(FileDomainConstant.StorageStatus.ACTIVE);
            objectDO.setRefCount(1);
            objectDO.setUploadTime(DateUtil.nowTime());
            objectDO.setLastAccessTime(DateUtil.nowTime());
            objectDO.setIsDelete(FileDomainConstant.LogicDelete.NO);
            objectDO.fillCreateTime(sessionVO.getTenantId(), sessionVO.getOrgId());
            fileObjectDao.insert(objectDO);

            FileRecordDO recordDO = buildFileRecord(sessionVO, sessionVO.getObjectId(), sessionVO.getFileSize());
            recordDO.setFileId(sessionVO.getFileId());
            fileRecordDao.insert(recordDO);
            result = new FileUploadVO();
            result.setFileId(recordDO.getFileId());
            result.setInstantUpload(Boolean.FALSE);
            result.setObjectReused(Boolean.FALSE);
            result.setFileName(recordDO.getFileName());
            result.setFileSize(recordDO.getFileSize());
        }
        fileUploadSessionDao.updateSessionStatus(sessionVO.getSessionId(), FileDomainConstant.SessionStatus.COMPLETED,
                DateUtil.nowTime(), currentOperator());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abortMultipartUpload(String sessionId) {
        FileUploadSessionVO sessionVO = requireSession(sessionId);
        if (FileDomainConstant.SessionStatus.COMPLETED.equals(sessionVO.getSessionStatus())
                || FileDomainConstant.SessionStatus.ABORTED.equals(sessionVO.getSessionStatus())) {
            return;
        }
        AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucketName(sessionVO.getBucketName())
                .objectKey(sessionVO.getObjectKey())
                .uploadId(sessionVO.getUploadId())
                .build();
        if (StringUtil.isBlank(sessionVO.getStorageProvider())) {
            multiZoneStorage.abortMultipartUpload(request);
        } else {
            multiZoneStorage.abortMultipartUpload(sessionVO.getStorageProvider(), request);
        }
        fileUploadSessionDao.updateSessionStatus(sessionVO.getSessionId(), FileDomainConstant.SessionStatus.ABORTED,
                DateUtil.nowTime(), currentOperator());
    }

    @Override
    public FileRecordVO selectByFileId(String fileId) {
        return fileRecordDao.selectDetailByFileId(fileId);
    }

    @Override
    public FileDownloadUrlVO getDownloadUrl(String fileId) {
        FileRecordVO recordVO = requireActiveRecord(fileId);
        PresignedUrlRequest request = PresignedUrlRequest.builder()
                .bucketName(recordVO.getBucketName())
                .objectKey(recordVO.getObjectKey())
                .expireSeconds(fileDomainProperties.getDownloadUrlExpireSeconds())
                .build();
        PresignedUrlResult urlResult = StringUtil.isBlank(recordVO.getStorageProvider())
                ? multiZoneStorage.generatePresignedUrl(request)
                : multiZoneStorage.generatePresignedUrl(recordVO.getStorageProvider(), request);
        FileDownloadUrlVO result = new FileDownloadUrlVO();
        result.setFileId(fileId);
        result.setUrl(urlResult.getUrl());
        result.setExpiresAt(format(urlResult.getExpiresAt()));
        touchObject(recordVO.getObjectId());
        return result;
    }

    @Override
    public PageResult<FileRecordVO> pageList(FileQueryQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<FileRecordVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> fileRecordDao.selectByQO(qo));
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logicalDelete(String fileId) {
        FileRecordVO recordVO = requireRecord(fileId);
        if (FileDomainConstant.LogicDelete.YES.equals(recordVO.getIsDelete())) {
            return;
        }
        String now = DateUtil.nowTime();
        String expireTime = format(LocalDateTime.now().plusDays(fileDomainProperties.getRetentionDays()));
        fileRecordDao.logicalDelete(fileId, FileDomainConstant.FileStatus.DELETED, now, expireTime,
                FileDomainConstant.LogicDelete.YES, now, currentOperator());
        fileObjectDao.decreaseRefCount(recordVO.getObjectId(), 1, now, currentOperator());
        if (fileRecordDao.countActiveByObjectId(recordVO.getObjectId()) <= 0) {
            fileObjectDao.updateStorageStatus(recordVO.getObjectId(), FileDomainConstant.StorageStatus.DELETE_PENDING,
                    FileDomainConstant.LogicDelete.NO, now, currentOperator());
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(String fileId) {
        FileRecordVO recordVO = requireRecord(fileId);
        if (!FileDomainConstant.LogicDelete.YES.equals(recordVO.getIsDelete())) {
            return;
        }
        if (StringUtil.isNotBlank(recordVO.getExpireDeleteTime())
                && recordVO.getExpireDeleteTime().compareTo(DateUtil.nowTime()) < 0) {
            throw new RuntimeException("file restore expired");
        }
        String now = DateUtil.nowTime();
        fileRecordDao.restoreByFileId(fileId, FileDomainConstant.FileStatus.ACTIVE,
                FileDomainConstant.LogicDelete.NO, now, currentOperator());
        fileObjectDao.increaseRefCount(recordVO.getObjectId(), 1, now, currentOperator());
        fileObjectDao.updateStorageStatus(recordVO.getObjectId(), FileDomainConstant.StorageStatus.ACTIVE,
                FileDomainConstant.LogicDelete.NO, now, currentOperator());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredDeletedFiles() {
        List<FileRecordVO> records = fileRecordDao.selectExpiredDeletedRecords(DateUtil.nowTime());
        for (FileRecordVO recordVO : records) {
            if (fileRecordDao.countActiveByObjectId(recordVO.getObjectId()) > 0) {
                continue;
            }
            deletePhysicalObject(recordVO.getStorageProvider(), recordVO.getBucketName(), recordVO.getObjectKey());
            fileObjectDao.updateStorageStatus(recordVO.getObjectId(), FileDomainConstant.StorageStatus.DELETED,
                    FileDomainConstant.LogicDelete.YES, DateUtil.nowTime(), FileDomainConstant.SYSTEM_OPERATOR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredUploadSessions() {
        List<FileUploadSessionVO> sessions = fileUploadSessionDao.selectExpiredSessions(DateUtil.nowTime());
        for (FileUploadSessionVO sessionVO : sessions) {
            try {
                AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                        .bucketName(sessionVO.getBucketName())
                        .objectKey(sessionVO.getObjectKey())
                        .uploadId(sessionVO.getUploadId())
                        .build();
                if (StringUtil.isBlank(sessionVO.getStorageProvider())) {
                    multiZoneStorage.abortMultipartUpload(request);
                } else {
                    multiZoneStorage.abortMultipartUpload(sessionVO.getStorageProvider(), request);
                }
            } catch (Exception ex) {
                log.error("abort expired upload session failed, sessionId={}", sessionVO.getSessionId(), ex);
            }
            fileUploadSessionDao.updateSessionStatus(sessionVO.getSessionId(), FileDomainConstant.SessionStatus.EXPIRED,
                    DateUtil.nowTime(), FileDomainConstant.SYSTEM_OPERATOR);
        }
    }

    private FileUploadVO createRecordFromExistingObject(FileUploadCheckDTO data, FileObjectVO objectVO) {
        FileRecordDO recordDO = buildFileRecord(data, objectVO.getObjectId(), data.getFileSize());
        fileRecordDao.insert(recordDO);
        fileObjectDao.increaseRefCount(objectVO.getObjectId(), 1, DateUtil.nowTime(), currentOperator());
        FileUploadVO result = new FileUploadVO();
        result.setFileId(recordDO.getFileId());
        result.setInstantUpload(Boolean.TRUE);
        result.setObjectReused(Boolean.TRUE);
        result.setFileName(recordDO.getFileName());
        result.setFileSize(recordDO.getFileSize());
        return result;
    }

    private FileUploadVO createRecordFromExistingSession(FileUploadSessionVO sessionVO, FileObjectVO objectVO) {
        FileRecordDO recordDO = buildFileRecord(sessionVO, objectVO.getObjectId(), sessionVO.getFileSize());
        recordDO.setFileId(sessionVO.getFileId());
        fileRecordDao.insert(recordDO);
        fileObjectDao.increaseRefCount(objectVO.getObjectId(), 1, DateUtil.nowTime(), currentOperator());
        FileUploadVO result = new FileUploadVO();
        result.setFileId(recordDO.getFileId());
        result.setInstantUpload(Boolean.FALSE);
        result.setObjectReused(Boolean.TRUE);
        result.setFileName(recordDO.getFileName());
        result.setFileSize(recordDO.getFileSize());
        return result;
    }

    private FileObjectDO buildFileObject(FileUploadCheckDTO data, String objectId, UploadResult uploadResult,
                                         String originFileName) {
        FileObjectDO objectDO = new FileObjectDO();
        objectDO.setObjectId(objectId);
        objectDO.setHashSha256(data.getSha256());
        objectDO.setHashMd5(data.getMd5());
        objectDO.setFileSize(data.getFileSize());
        objectDO.setStorageProvider(uploadResult.getProviderName());
        objectDO.setBucketName(uploadResult.getBucketName());
        objectDO.setObjectKey(uploadResult.getObjectKey());
        objectDO.setOriginFileName(StringUtil.isNotBlank(originFileName) ? originFileName : data.getFileName());
        objectDO.setContentType(resolveContentType(data.getContentType(), null));
        objectDO.setExtension(extractExtension(data.getFileName()));
        objectDO.setStorageStatus(FileDomainConstant.StorageStatus.ACTIVE);
        objectDO.setRefCount(1);
        objectDO.setUploadTime(DateUtil.nowTime());
        objectDO.setLastAccessTime(DateUtil.nowTime());
        objectDO.setIsDelete(FileDomainConstant.LogicDelete.NO);
        objectDO.fillCreateTime();
        return objectDO;
    }

    private FileRecordDO buildFileRecord(FileUploadCheckDTO data, String objectId, Long fileSize) {
        FileRecordDO recordDO = new FileRecordDO();
        recordDO.setFileId(IDGeneratorUtil.UUID());
        recordDO.setObjectId(objectId);
        recordDO.setBizType(data.getBizType());
        recordDO.setBizId(data.getBizId());
        recordDO.setBizTag(data.getBizTag());
        recordDO.setFileName(data.getFileName());
        recordDO.setDisplayName(StringUtil.isNotBlank(data.getDisplayName()) ? data.getDisplayName() : data.getFileName());
        recordDO.setContentType(data.getContentType());
        recordDO.setFileSize(fileSize);
        recordDO.setFileExt(extractExtension(data.getFileName()));
        recordDO.setFileStatus(FileDomainConstant.FileStatus.ACTIVE);
        recordDO.setRemark(data.getRemark());
        recordDO.setIsDelete(FileDomainConstant.LogicDelete.NO);
        recordDO.fillCreateTime();
        return recordDO;
    }

    private FileRecordDO buildFileRecord(FileUploadSessionVO sessionVO, String objectId, Long fileSize) {
        FileRecordDO recordDO = new FileRecordDO();
        recordDO.setFileId(IDGeneratorUtil.UUID());
        recordDO.setObjectId(objectId);
        recordDO.setBizType(sessionVO.getBizType());
        recordDO.setBizId(sessionVO.getBizId());
        recordDO.setBizTag(sessionVO.getBizTag());
        recordDO.setFileName(sessionVO.getFileName());
        recordDO.setDisplayName(StringUtil.isNotBlank(sessionVO.getDisplayName()) ? sessionVO.getDisplayName() : sessionVO.getFileName());
        recordDO.setContentType(sessionVO.getContentType());
        recordDO.setFileSize(fileSize);
        recordDO.setFileExt(extractExtension(sessionVO.getFileName()));
        recordDO.setFileStatus(FileDomainConstant.FileStatus.ACTIVE);
        recordDO.setRemark(sessionVO.getRemark());
        recordDO.setIsDelete(FileDomainConstant.LogicDelete.NO);
        recordDO.fillCreateTime(sessionVO.getTenantId(), sessionVO.getOrgId());
        return recordDO;
    }

    private void validateCheckData(FileUploadCheckDTO data) {
        if (data == null) {
            throw new RuntimeException("request data is empty");
        }
        if (StringUtil.isBlank(data.getSha256())) {
            throw new RuntimeException("sha256 is empty");
        }
        if (data.getFileSize() == null || data.getFileSize() < 0) {
            throw new RuntimeException("file size is invalid");
        }
        if (StringUtil.isBlank(data.getFileName())) {
            throw new RuntimeException("file name is empty");
        }
        if (StringUtil.isBlank(data.getBizType())) {
            throw new RuntimeException("biz type is empty");
        }
    }

    private FileRecordVO requireRecord(String fileId) {
        FileRecordVO recordVO = fileRecordDao.selectDetailByFileId(fileId);
        if (recordVO == null) {
            throw new RuntimeException("file not found");
        }
        return recordVO;
    }

    private FileRecordVO requireActiveRecord(String fileId) {
        FileRecordVO recordVO = requireRecord(fileId);
        if (FileDomainConstant.LogicDelete.YES.equals(recordVO.getIsDelete())
                || !FileDomainConstant.FileStatus.ACTIVE.equals(recordVO.getFileStatus())) {
            throw new RuntimeException("file is unavailable");
        }
        return recordVO;
    }

    private FileUploadSessionVO requireSession(String sessionId) {
        FileUploadSessionVO sessionVO = fileUploadSessionDao.selectById(sessionId);
        if (sessionVO == null) {
            throw new RuntimeException("upload session not found");
        }
        return sessionVO;
    }

    private FileUploadSessionVO requireActiveSession(String sessionId) {
        FileUploadSessionVO sessionVO = requireSession(sessionId);
        if (FileDomainConstant.LogicDelete.YES.equals(sessionVO.getIsDelete())) {
            throw new RuntimeException("upload session deleted");
        }
        if (FileDomainConstant.SessionStatus.ABORTED.equals(sessionVO.getSessionStatus())
                || FileDomainConstant.SessionStatus.EXPIRED.equals(sessionVO.getSessionStatus())
                || FileDomainConstant.SessionStatus.FAILED.equals(sessionVO.getSessionStatus())) {
            throw new RuntimeException("upload session is unavailable");
        }
        return sessionVO;
    }

    private UploadResult uploadObject(String providerName, UploadObjectRequest request) {
        if (StringUtil.isBlank(providerName)) {
            return multiZoneStorage.upload(request);
        }
        return multiZoneStorage.upload(providerName, request);
    }

    private InitiateMultipartUploadResult initiateMultipart(String providerName, String objectKey, String contentType) {
        InitiateMultipartUploadRequest request = InitiateMultipartUploadRequest.builder()
                .objectKey(objectKey)
                .contentType(contentType)
                .build();
        if (StringUtil.isBlank(providerName)) {
            return multiZoneStorage.initiateMultipartUpload(request);
        }
        return multiZoneStorage.initiateMultipartUpload(providerName, request);
    }

    private UploadPartResult preparePart(FileUploadSessionVO sessionVO, Integer partNumber) {
        UploadPartRequest request = UploadPartRequest.builder()
                .bucketName(sessionVO.getBucketName())
                .objectKey(sessionVO.getObjectKey())
                .uploadId(sessionVO.getUploadId())
                .partNumber(partNumber)
                .expireSeconds(fileDomainProperties.getPartUrlExpireSeconds())
                .build();
        if (StringUtil.isBlank(sessionVO.getStorageProvider())) {
            return multiZoneStorage.prepareUploadPart(request);
        }
        return multiZoneStorage.prepareUploadPart(sessionVO.getStorageProvider(), request);
    }

    private CompleteMultipartUploadResult completeMultipart(FileUploadSessionVO sessionVO, List<FileMultipartCompletePartDTO> parts) {
        CompleteMultipartUploadRequest.Builder builder = CompleteMultipartUploadRequest.builder()
                .bucketName(sessionVO.getBucketName())
                .objectKey(sessionVO.getObjectKey())
                .uploadId(sessionVO.getUploadId());
        for (FileMultipartCompletePartDTO part : parts) {
            builder.addPart(part.getPartNumber(), part.getETag());
        }
        CompleteMultipartUploadRequest request = builder.build();
        if (StringUtil.isBlank(sessionVO.getStorageProvider())) {
            return multiZoneStorage.completeMultipartUpload(request);
        }
        return multiZoneStorage.completeMultipartUpload(sessionVO.getStorageProvider(), request);
    }

    private void verifyDigest(FileUploadCheckDTO data, byte[] bytes) {
        String sha256 = digest(FileDomainConstant.DIGEST_SHA256_ALGORITHM, bytes);
        if (!data.getSha256().equalsIgnoreCase(sha256)) {
            throw new RuntimeException("sha256 verify failed");
        }
        if (StringUtil.isNotBlank(data.getMd5())) {
            String md5 = digest(FileDomainConstant.DIGEST_MD5_ALGORITHM, bytes);
            if (!data.getMd5().equalsIgnoreCase(md5)) {
                throw new RuntimeException("md5 verify failed");
            }
        }
    }

    private void verifyUploadedObjectDigest(FileUploadSessionVO sessionVO) {
        DownloadObjectRequest request = DownloadObjectRequest.builder()
                .bucketName(sessionVO.getBucketName())
                .objectKey(sessionVO.getObjectKey())
                .build();
        try (InputStream inputStream = StringUtil.isBlank(sessionVO.getStorageProvider())
                ? multiZoneStorage.download(request)
                : multiZoneStorage.download(sessionVO.getStorageProvider(), request)) {
            MessageDigest sha256 = MessageDigest.getInstance(FileDomainConstant.DIGEST_SHA256_ALGORITHM);
            byte[] buffer = new byte[FileDomainConstant.BUFFER_SIZE];
            int len;
            long total = 0L;
            while ((len = inputStream.read(buffer)) != -1) {
                sha256.update(buffer, 0, len);
                total += len;
            }
            String digest = toHex(sha256.digest());
            if (!sessionVO.getHashSha256().equalsIgnoreCase(digest)) {
                throw new RuntimeException("uploaded object sha256 verify failed");
            }
            if (sessionVO.getFileSize() != null && !sessionVO.getFileSize().equals(total)) {
                throw new RuntimeException("uploaded object size mismatch");
            }
        } catch (Exception ex) {
            fileUploadSessionDao.updateSessionStatus(sessionVO.getSessionId(), FileDomainConstant.SessionStatus.FAILED,
                    DateUtil.nowTime(), currentOperator());
            throw new RuntimeException(ex);
        }
    }

    private void deletePhysicalObject(String providerName, String bucketName, String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucketName(bucketName)
                .objectKey(objectKey)
                .build();
        if (StringUtil.isBlank(providerName)) {
            multiZoneStorage.delete(request);
        } else {
            multiZoneStorage.delete(providerName, request);
        }
    }

    private void touchObject(String objectId) {
        FileObjectDO update = new FileObjectDO();
        update.setObjectId(objectId);
        update.setLastAccessTime(DateUtil.nowTime());
        update.setModifyTime(DateUtil.nowTime());
        update.setModifierId(currentOperator());
        fileObjectDao.updateById(update);
    }

    private String resolveProvider(String providerName) {
        if (StringUtil.isNotBlank(providerName)) {
            return providerName;
        }
        if (StringUtil.isNotBlank(fileDomainProperties.getDefaultProvider())) {
            return fileDomainProperties.getDefaultProvider();
        }
        return null;
    }

    private String resolveContentType(String preferred, String fallback) {
        if (StringUtil.isNotBlank(preferred)) {
            return preferred;
        }
        if (StringUtil.isNotBlank(fallback)) {
            return fallback;
        }
        return FileDomainConstant.DEFAULT_CONTENT_TYPE;
    }

    private String buildObjectKey(String bizType, String fileName) {
        String ext = extractExtension(fileName);
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern(FileDomainConstant.OBJECT_KEY_DATE_PATTERN));
        StringBuilder key = new StringBuilder();
        key.append(fileDomainProperties.getObjectKeyPrefix()).append(FileDomainConstant.OBJECT_KEY_SEPARATOR)
                .append(normalizePathSegment(bizType)).append(FileDomainConstant.OBJECT_KEY_SEPARATOR)
                .append(datePart).append(FileDomainConstant.OBJECT_KEY_SEPARATOR)
                .append(IDGeneratorUtil.UUID());
        if (StringUtil.isNotBlank(ext)) {
            key.append(".").append(ext.toLowerCase(Locale.ROOT));
        }
        return key.toString();
    }

    private String normalizePathSegment(String value) {
        String source = StringUtil.isBlank(value) ? FileDomainConstant.DEFAULT_BIZ_TYPE : value;
        return source.replaceAll(FileDomainConstant.BIZ_TYPE_ALLOWED_PATTERN, "_");
    }

    private String extractExtension(String fileName) {
        if (StringUtil.isBlank(fileName) || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String digest(String algorithm, byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(bytes);
            return toHex(messageDigest.digest());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private String currentOperator() {
        UserContext context = SecurityContextHolder.get();
        if (context == null || StringUtil.isBlank(context.getUserId())) {
            return FileDomainConstant.SYSTEM_OPERATOR;
        }
        return context.getUserId();
    }

    private void fillCurrentTenantOrg(FileQueryQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private String requireTenantId() {
        String tenantId = SecurityContextHolder.currentTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new IllegalStateException("Current tenant context is missing");
        }
        return tenantId;
    }

    private String requireOrgId() {
        String orgId = SecurityContextHolder.currentOrgId();
        if (StringUtil.isBlank(orgId)) {
            throw new IllegalStateException("Current organization context is missing");
        }
        return orgId;
    }

    private String format(LocalDateTime localDateTime) {
        return localDateTime.format(DATE_TIME_FORMATTER);
    }

    private String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }
}
