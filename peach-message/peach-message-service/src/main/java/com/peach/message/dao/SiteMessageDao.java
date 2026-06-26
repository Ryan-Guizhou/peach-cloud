package com.peach.message.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.message.entity.SiteMessageDO;
import com.peach.message.qo.SiteMessageQO;
import com.peach.message.vo.SiteMessageVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 站内信数据访问接口
 */
@Indexed
@MybatisDao
public interface SiteMessageDao extends PeachDao<SiteMessageDO, SiteMessageVO> {

    /**
     * 根据查询参数对象查询站内信列表
     *
     * @param qo 站内信查询参数对象
     * @return 站内信视图对象列表
     */
    List<SiteMessageVO> selectByQO(SiteMessageQO qo);

    /**
     * 查询用户未读数
     *
     * @param receiverId 接收人ID
     * @return 未读数
     */
    Long countUnread(@Param("receiverId") String receiverId,
                     @Param("messageTypeList") List<String> messageTypeList,
                     @Param("messageType") String messageType);

    /**
     * 根据来源编码撤销站内信
     *
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     */
    void revokeBySource(@Param("sourceType") String sourceType,
                        @Param("sourceCode") String sourceCode);

    /**
     * 标记用户全部站内信已读
     *
     * @param receiverId 接收人ID
     * @param modifyTime 修改时间
     */
    void readAllByReceiver(@Param("receiverId") String receiverId,
                           @Param("messageTypeList") List<String> messageTypeList,
                           @Param("messageType") String messageType,
                           @Param("modifyTime") String modifyTime);
}
