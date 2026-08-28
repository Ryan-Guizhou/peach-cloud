package com.peach.fileservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.fileservice.entity.FileObjectDO;
import com.peach.fileservice.vo.FileObjectVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 文件Object数据访问。
 * <p>提供文件对象（存储实体）的数据库操作，包括秒传检测、引用计数管理、
 * 存储状态更新等功能。文件对象与文件记录是一对多关系。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Indexed
@MybatisDao
public interface FileObjectDao extends PeachDao<FileObjectDO, FileObjectVO> {

    /**
     * 根据SHA256和文件大小查询活跃的文件对象
     *
     * <p>用于秒传检测，判断相同内容的文件是否已存在。</p>
     *
     * @param hashSha256 文件SHA256哈希值
     * @param fileSize 文件大小（字节）
     * @return 活跃的文件对象，不存在时返回null
     */
    FileObjectVO selectActiveBySha256AndSize(@Param("hashSha256") String hashSha256, @Param("fileSize") Long fileSize);

    /**
     * 增加文件对象的引用计数
     *
     * <p>当新的文件记录引用已有文件对象时调用，用于跟踪对象的使用情况。</p>
     *
     * @param objectId 文件对象ID
     * @param count 增加的数量
     * @param modifyTime 修改时间
     * @param modifier 修改人
     */
    void increaseRefCount(@Param("objectId") String objectId, @Param("count") Integer count,
                          @Param("modifyTime") String modifyTime, @Param("modifier") String modifier);

    /**
     * 减少文件对象的引用计数
     *
     * <p>当文件记录被删除时调用，引用计数为0时可物理删除存储对象。</p>
     *
     * @param objectId 文件对象ID
     * @param count 减少的数量
     * @param modifyTime 修改时间
     * @param modifier 修改人
     */
    void decreaseRefCount(@Param("objectId") String objectId, @Param("count") Integer count,
                          @Param("modifyTime") String modifyTime, @Param("modifier") String modifier);

    /**
     * 更新文件对象的存储状态
     *
     * @param objectId 文件对象ID
     * @param storageStatus 存储状态（UPLOADING/ACTIVE/DELETE_PENDING/DELETED/UPLOAD_FAILED）
     * @param isDelete 删除标记
     * @param modifyTime 修改时间
     * @param modifier 修改人
     */
    void updateStorageStatus(@Param("objectId") String objectId, @Param("storageStatus") String storageStatus,
                             @Param("isDelete") Integer isDelete, @Param("modifyTime") String modifyTime,
                             @Param("modifier") String modifier);

    /**
     * 根据文件对象ID列表批量查询
     *
     * @param objectIds 文件对象ID列表
     * @return 文件对象视图对象列表
     */
    List<FileObjectVO> selectByObjectIds(@Param("objectIds") List<String> objectIds);
}
