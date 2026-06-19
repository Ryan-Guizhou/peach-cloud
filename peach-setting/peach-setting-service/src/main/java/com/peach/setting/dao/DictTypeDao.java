package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.DictTypeDO;
import com.peach.setting.qo.DictTypeQO;
import com.peach.setting.vo.DictTypeVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 字典类型数据访问接口
 */
@Indexed
@MybatisDao
public interface DictTypeDao extends PeachDao<DictTypeDO, DictTypeVO> {

    /**
     * 根据查询参数对象查询字典类型列表
     *
     * @param qo 字典类型查询参数对象
     * @return 字典类型视图对象列表
     */
    List<DictTypeVO> selectByQO(DictTypeQO qo);
}
