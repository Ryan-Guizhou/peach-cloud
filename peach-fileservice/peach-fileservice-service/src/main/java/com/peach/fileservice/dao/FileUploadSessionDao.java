package com.peach.fileservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.fileservice.entity.FileUploadSessionDO;
import com.peach.fileservice.vo.FileUploadSessionVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 文件上传会话数据访问。
 * <p>提供分片上传会话的数据库操作，包括会话状态更新、过期会话查询等功能。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Indexed
@MybatisDao
public interface FileUploadSessionDao extends PeachDao<FileUploadSessionDO, FileUploadSessionVO> {

    /**
     * 更新上传会话状态
     *
     * @param sessionId 会话ID
     * @param sessionStatus 会话状态（INITIATED/UPLOADING/COMPLETED/ABORTED/EXPIRED/FAILED）
     * @param modifyTime 修改时间
     * @param modifier 修改人
     */
    void updateSessionStatus(@Param("sessionId") String sessionId, @Param("sessionStatus") String sessionStatus,
                             @Param("modifyTime") String modifyTime, @Param("modifier") String modifier);

    /**
     * 查询过期的上传会话列表
     *
     * <p>用于定时任务清理超时未完成的上传会话，释放临时资源。</p>
     *
     * @param nowTime 当前时间，用于判断是否过期
     * @return 过期的上传会话列表
     */
    List<FileUploadSessionVO> selectExpiredSessions(@Param("nowTime") String nowTime);
}
