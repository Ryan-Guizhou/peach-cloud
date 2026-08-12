package com.peach.fileservice.service;

import com.peach.common.PageResult;
import com.peach.fileservice.dto.FileMultipartCompleteDTO;
import com.peach.fileservice.dto.FileMultipartInitDTO;
import com.peach.fileservice.dto.FileMultipartPartUrlDTO;
import com.peach.fileservice.dto.FileExternalUploadDTO;
import com.peach.fileservice.dto.FileUploadCheckDTO;
import com.peach.fileservice.qo.FileQueryQO;
import com.peach.fileservice.vo.FileDownloadUrlVO;
import com.peach.fileservice.vo.FileDigestVO;
import com.peach.fileservice.vo.FileExternalFileVO;
import com.peach.fileservice.vo.FileMultipartInitVO;
import com.peach.fileservice.vo.FileMultipartPartVO;
import com.peach.fileservice.vo.FileRecordVO;
import com.peach.fileservice.vo.FileUploadCheckVO;
import com.peach.fileservice.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件域服务接口
 *
 * <p>提供文件上传、下载、分片上传、文件管理等核心业务功能。
 * 支持秒传检测、分片上传、逻辑删除、文件恢复等高级特性。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
public interface IFileDomainService {

    /**
     * 计算上传文件的 SHA-256 摘要。
     *
     * @param file multipart 文件
     * @return 摘要和文件大小
     */
    FileDigestVO calculateSha256(MultipartFile file);

    /**
     * 处理外部上传请求。服务端负责计算摘要并复用内部上传流程。
     *
     * @param data 外部上传参数
     * @param file multipart 文件
     * @return 上传结果
     */
    FileUploadVO uploadExternal(FileExternalUploadDTO data, MultipartFile file);

    /**
     * 查询不包含存储定位信息的外部文件详情。
     *
     * @param fileId 业务文件ID
     * @return 外部文件详情
     */
    FileExternalFileVO selectExternalByFileId(String fileId);

    /**
     * 文件上传前校验
     *
     * <p>检查文件是否已存在（秒传检测），验证文件合法性，
     * 返回上传会话信息和预签名URL。</p>
     *
     * @param data 文件上传校验数据传输对象，包含文件SHA256、大小、名称等信息
     * @return 文件上传校验结果，包含是否秒传、会话ID、预签名URL等
     */
    FileUploadCheckVO uploadCheck(FileUploadCheckDTO data);

    /**
     * 简单文件上传
     *
     * <p>适用于小文件的直接上传，一次性完成文件传输。</p>
     *
     * @param data 文件上传校验数据传输对象
     * @param file 上传的文件对象
     * @return 文件上传结果，包含文件ID、访问URL等信息
     */
    FileUploadVO upload(FileUploadCheckDTO data, MultipartFile file);

    /**
     * 初始化分片上传
     *
     * <p>创建分片上传会话，返回uploadId和预签名URL列表，
     * 客户端根据返回的URL列表逐个上传分片。</p>
     *
     * @param data 分片上传初始化数据传输对象，包含文件信息和分片配置
     * @return 分片上传初始化结果，包含uploadId、会话ID、分片预签名URL列表
     */
    FileMultipartInitVO initMultipartUpload(FileMultipartInitDTO data);

    /**
     * 准备分片上传URL
     *
     * <p>为指定分片生成预签名上传URL，用于客户端直传分片数据。</p>
     *
     * @param data 分片URL数据传输对象，包含会话ID和分片编号
     * @return 分片URL结果，包含预签名上传URL和过期时间
     */
    FileMultipartPartVO prepareMultipartPart(FileMultipartPartUrlDTO data);

    /**
     * 完成分片上传
     *
     * <p>所有分片上传完成后调用此方法，合并分片并创建文件记录。</p>
     *
     * @param data 分片完成数据传输对象，包含会话ID和所有分片的ETag信息
     * @return 文件上传结果，包含文件ID、访问URL等信息
     */
    FileUploadVO completeMultipartUpload(FileMultipartCompleteDTO data);

    /**
     * 中止分片上传
     *
     * <p>取消正在进行的分片上传任务，清理已上传的分片和会话数据。</p>
     *
     * @param sessionId 上传会话ID
     */
    void abortMultipartUpload(String sessionId);

    /**
     * 根据文件ID查询文件记录
     *
     * @param fileId 文件ID
     * @return 文件记录视图对象，不存在时返回null
     */
    FileRecordVO selectByFileId(String fileId);

    /**
     * 获取文件下载URL
     *
     * <p>生成文件的预签名下载URL，支持设置过期时间。</p>
     *
     * @param fileId 文件ID
     * @return 文件下载URL结果，包含预签名下载URL和过期时间
     */
    FileDownloadUrlVO getDownloadUrl(String fileId);

    /**
     * 分页查询文件记录列表
     *
     * @param qo 文件查询参数对象，支持按文件名、类型、时间等条件查询
     * @return 文件记录分页结果
     */
    PageResult<FileRecordVO> pageList(FileQueryQO qo);

    /**
     * 逻辑删除文件
     *
     * <p>将文件标记为已删除状态，不立即从存储中移除，
     * 支持在保留期内恢复。过期后由定时任务物理删除。</p>
     *
     * @param fileId 文件ID
     */
    void logicalDelete(String fileId);

    /**
     * 恢复已删除的文件
     *
     * <p>将逻辑删除的文件恢复为正常状态，仅在保留期内有效。</p>
     *
     * @param fileId 文件ID
     */
    void restore(String fileId);

    /**
     * 清理过期的已删除文件
     *
     * <p>定时任务调用，物理删除超过保留期的逻辑删除文件，
     * 释放存储空间。同时清理相关的文件对象引用计数。</p>
     */
    void cleanupExpiredDeletedFiles();

    /**
     * 清理过期的上传会话
     *
     * <p>定时任务调用，清理超时未完成的上片上传会话，
     * 释放临时资源，避免存储泄漏。</p>
     */
    void cleanupExpiredUploadSessions();
}
