package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.NoticeReadRecordDO;
import com.peach.setting.qo.NoticeReadRecordQO;
import com.peach.setting.vo.NoticeReadRecordVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 公告阅读记录数据访问接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 公告阅读记录数据访问接口
 */
@Indexed
@MybatisDao
public interface NoticeReadRecordDao extends PeachDao<NoticeReadRecordDO, NoticeReadRecordVO> {

    /**
     * 根据查询参数对象查询公告已读记录列表
     *
     * @param qo 公告已读记录查询参数对象
     * @return 公告已读记录视图对象列表
     */
    List<NoticeReadRecordVO> selectByQO(NoticeReadRecordQO qo);

    /**
     * 根据公告编码和用户ID查询公告已读记录
     *
     * @param noticeCode  公告编码
     * @param readUserId  阅读用户ID
     * @return 公告已读记录视图对象，不存在时返回null
     */
    NoticeReadRecordVO selectByNoticeCodeAndUserId(@Param("noticeCode") String noticeCode, @Param("readUserId") String readUserId);
}
