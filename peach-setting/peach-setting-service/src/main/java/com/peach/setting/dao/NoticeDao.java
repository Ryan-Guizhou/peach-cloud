package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.NoticeDO;
import com.peach.setting.qo.NoticeQO;
import com.peach.setting.vo.NoticeVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 公告数据访问接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 公告数据访问接口
 */
@Indexed
@MybatisDao
public interface NoticeDao extends PeachDao<NoticeDO, NoticeVO> {

    /**
     * 根据查询参数对象查询公告列表
     *
     * @param qo 公告查询参数对象
     * @return 公告视图对象列表
     */
    List<NoticeVO> selectByQO(NoticeQO qo);

    NoticeVO selectByNoticeCode(@Param("noticeCode") String noticeCode);

    /**
     * 增加公告的阅读次数
     *
     * @param noticeCode 公告编码
     * @param count      增加的阅读次数
     */
    void increaseReadCount(@Param("noticeCode") String noticeCode, @Param("count") Integer count);
}
