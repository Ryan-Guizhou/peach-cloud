package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.ValueSetDO;
import com.peach.setting.qo.ValueSetQO;
import com.peach.setting.vo.ValueSetVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 值集数据访问接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 值集数据访问接口
 */
@Indexed
@MybatisDao
public interface ValueSetDao extends PeachDao<ValueSetDO, ValueSetVO> {

    /**
     * 根据查询参数对象查询值集列表
     *
     * @param qo 值集查询参数对象
     * @return 值集视图对象列表
     */
    List<ValueSetVO> selectByQO(ValueSetQO qo);
}
