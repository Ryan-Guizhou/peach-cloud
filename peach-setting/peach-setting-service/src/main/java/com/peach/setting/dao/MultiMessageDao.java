package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.MultiMessageDO;
import com.peach.setting.qo.MulitMessageQO;
import com.peach.setting.vo.MulitMessageVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 多语言消息数据访问接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 多语言消息数据访问接口
 */
@Indexed
@MybatisDao
public interface MultiMessageDao extends PeachDao<MultiMessageDO, MulitMessageVO> {

    /**
     * 根据查询参数对象查询多语言消息列表
     *
     * @param qo 多语言消息查询参数对象
     * @return 多语言消息视图对象列表
     */
    List<MulitMessageVO> selectByQO(MulitMessageQO qo);

    /**
     * 根据消息Key查询多语言消息列表
     *
     * @param messageKey 消息Key
     * @return 多语言消息视图对象列表
     */
    List<MulitMessageVO> selectByMessageKey(@Param("messageKey") String messageKey);
}
