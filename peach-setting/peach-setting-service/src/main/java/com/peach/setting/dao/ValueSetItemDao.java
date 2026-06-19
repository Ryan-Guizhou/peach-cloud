package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.ValueSetItemDO;
import com.peach.setting.qo.ValueSetItemQO;
import com.peach.setting.vo.ValueSetItemVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 值集项数据访问接口
 */
@Indexed
@MybatisDao
public interface ValueSetItemDao extends PeachDao<ValueSetItemDO, ValueSetItemVO> {

    /**
     * 根据查询参数对象查询值集项列表
     *
     * @param qo 值集项查询参数对象
     * @return 值集项视图对象列表
     */
    List<ValueSetItemVO> selectByQO(ValueSetItemQO qo);

    /**
     * 根据值集编码查询值集项列表
     *
     * @param valueSetCode 值集编码
     * @return 值集项视图对象列表
     */
    List<ValueSetItemVO> selectByValueSetCode(@Param("valueSetCode") String valueSetCode);

    /**
     * 根据值集编码删除值集项
     *
     * @param valueSetCode 值集编码
     */
    void delByValueSetCode(@Param("valueSetCode") String valueSetCode);
}
