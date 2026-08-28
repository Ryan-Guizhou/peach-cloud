package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.LanguageDO;
import com.peach.setting.qo.LanguageQO;
import com.peach.setting.vo.LanguageVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 语言配置数据访问接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:00
 * @Description 语言配置数据访问接口
 */
@Indexed
@MybatisDao
public interface LanguageDao extends PeachDao<LanguageDO, LanguageVO> {

    /**
     * 根据查询参数对象查询语言列表
     *
     * @param qo 语言查询参数对象
     * @return 语言视图对象列表
     */
    List<LanguageVO> selectByQO(LanguageQO qo);

    /**
     * 清空所有语言的默认标记
     */
    void clearDefaultFlag();
}
