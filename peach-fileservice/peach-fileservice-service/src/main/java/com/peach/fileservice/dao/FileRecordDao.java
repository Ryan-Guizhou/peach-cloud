package com.peach.fileservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.fileservice.entity.FileRecordDO;
import com.peach.fileservice.qo.FileQueryQO;
import com.peach.fileservice.vo.FileRecordVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;
import java.util.Map;

/**
 * 文件记录数据访问。
 * <p>提供文件记录的数据库操作，包括查询、逻辑删除、恢复、清理等功能。
 * 支持文件的完整生命周期管理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Indexed
@MybatisDao
public interface FileRecordDao extends PeachDao<FileRecordDO, FileRecordVO> {

    /**
     * 根据查询条件分页查询文件记录列表
     *
     * @param qo 文件查询参数对象，支持按文件名、类型、状态、时间范围等条件查询
     * @return 文件记录视图对象列表
     */
    List<FileRecordVO> selectByQO(FileQueryQO qo);

    /**
     * SQL 强制物理租户和应用，完整所有者/机构权限由 Auth 单对象策略校验。
     */
    FileRecordVO selectScopedDetail(@Param("fileId") String fileId,
                                    @Param("tenantRefId") String tenantRefId,
                                    @Param("appRefId") String appRefId);

    /**
     * 固定 SQL 只修改白名单列，并再次限制物理租户、应用、对象所有者和机构状态。
     *
     * @return 影响行数，必须恰好等于 1
     */
    int updateScopedMetadata(@Param("fileId") String fileId,
                             @Param("tenantRefId") String tenantRefId,
                             @Param("appRefId") String appRefId,
                             @Param("ownerUserRefId") String ownerUserRefId,
                             @Param("orgRefId") String orgRefId,
                             @Param("changes") Map<String, String> changes,
                             @Param("modifyTime") String modifyTime,
                             @Param("modifierId") String modifierId);

    /**
     * 统计指定文件对象的活跃引用数量
     *
     * <p>用于判断文件对象是否仍被使用，决定是否物理删除存储对象。</p>
     *
     * @param objectId 文件对象ID
     * @return 活跃引用数量
     */
    int countActiveByObjectId(@Param("objectId") String objectId);

    /**
     * 逻辑删除文件记录
     *
     * <p>将文件标记为已删除状态，设置过期删除时间，
     * 在保留期内可通过恢复操作还原。</p>
     *
     * @param fileId 文件ID
     * @param fileStatus 文件状态（设置为 DELETED）
     * @param deleteTime 删除时间
     * @param expireDeleteTime 过期删除时间（超过此时间后物理删除）
     * @param isDelete 删除标记（1-已删除）
     * @param modifyTime 修改时间
     * @param modifier 修改人
     */
    int logicalDelete(@Param("fileId") String fileId,
                      @Param("tenantRefId") String tenantRefId,
                      @Param("appRefId") String appRefId,
                      @Param("ownerUserRefId") String ownerUserRefId,
                      @Param("orgRefId") String orgRefId,
                      @Param("fileStatus") String fileStatus,
                       @Param("deleteTime") String deleteTime, @Param("expireDeleteTime") String expireDeleteTime,
                       @Param("isDelete") Integer isDelete, @Param("modifyTime") String modifyTime,
                       @Param("modifier") String modifier);

    /**
     * 根据文件ID恢复已删除的文件
     *
     * <p>将逻辑删除的文件恢复为正常状态，仅在保留期内有效。</p>
     *
     * @param fileId 文件ID
     * @param fileStatus 文件状态（恢复为 ACTIVE）
     * @param isDelete 删除标记（0-未删除）
     * @param modifyTime 修改时间
     * @param modifier 修改人
     */
    int restoreByFileId(@Param("fileId") String fileId,
                        @Param("tenantRefId") String tenantRefId,
                        @Param("appRefId") String appRefId,
                        @Param("ownerUserRefId") String ownerUserRefId,
                        @Param("orgRefId") String orgRefId,
                        @Param("fileStatus") String fileStatus,
                         @Param("isDelete") Integer isDelete, @Param("modifyTime") String modifyTime,
                         @Param("modifier") String modifier);

    /**
     * 查询过期的已删除文件记录
     *
     * <p>用于定时任务物理删除超过保留期的文件记录，释放存储空间。</p>
     *
     * @param nowTime 当前时间，用于判断是否超过过期删除时间
     * @return 过期的已删除文件记录列表
     */
    List<FileRecordVO> selectExpiredDeletedRecords(@Param("nowTime") String nowTime);
}
