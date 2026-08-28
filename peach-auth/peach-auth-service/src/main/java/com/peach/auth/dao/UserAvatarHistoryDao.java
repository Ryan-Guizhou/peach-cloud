package com.peach.auth.dao;

import com.peach.auth.entity.UserAvatarHistoryDO;
import com.peach.auth.vo.AvatarHistoryVO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 用户头像History数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Indexed
@MybatisDao
public interface UserAvatarHistoryDao extends PeachDao<UserAvatarHistoryDO, UserAvatarHistoryDO> {

    List<AvatarHistoryVO> selectActiveByUserId(@Param("userId") String userId);

    int updateOrder(@Param("avatarHistoryId") String avatarHistoryId,
                    @Param("userId") String userId,
                    @Param("sortNo") Integer sortNo,
                    @Param("isCurrent") Integer isCurrent,
                    @Param("modifyTime") String modifyTime,
                    @Param("modifierId") String modifierId);

    int markDeleted(@Param("avatarHistoryIds") List<String> avatarHistoryIds,
                    @Param("userId") String userId,
                    @Param("modifyTime") String modifyTime,
                    @Param("modifierId") String modifierId);
}
