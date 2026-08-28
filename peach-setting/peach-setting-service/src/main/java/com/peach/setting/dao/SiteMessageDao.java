package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.SiteMessageDO;
import com.peach.setting.qo.SiteMessageQO;
import com.peach.setting.vo.SiteMessageVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 站内信数据访问接口。
 *
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
     * 根据来源编码撤销站内信
     *
     * @param sourceCode 来源编码（如公告编码等）
     */
    void revokeBySourceCode(@Param("sourceCode") String sourceCode);
}
