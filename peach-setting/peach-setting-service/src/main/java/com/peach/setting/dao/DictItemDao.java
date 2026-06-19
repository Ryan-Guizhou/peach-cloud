package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.DictItemDO;
import com.peach.setting.qo.DictItemQO;
import com.peach.setting.vo.DictItemVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 字典项数据访问接口
 */
@Indexed
@MybatisDao
public interface DictItemDao extends PeachDao<DictItemDO, DictItemVO> {

    /**
     * 根据查询参数对象查询字典项列表
     *
     * @param qo 字典项查询参数对象
     * @return 字典项视图对象列表
     */
    List<DictItemVO> selectByQO(DictItemQO qo);

    /**
     * 根据字典编码查询字典项列表
     *
     * @param dictCode 字典编码
     * @return 字典项视图对象列表
     */
    List<DictItemVO> selectByDictCode(@Param("dictCode") String dictCode);

    /**
     * 根据字典编码删除字典项
     *
     * @param dictCode 字典编码
     */
    void delByDictCode(@Param("dictCode") String dictCode);
}
