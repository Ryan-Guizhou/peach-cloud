package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.ValueSetDO;
import com.peach.setting.vo.ValueSetVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 17:59
 */
@Indexed
@MybatisDao
public interface ValueSetItemDao extends PeachDao<ValueSetDO, ValueSetVO> {

}
