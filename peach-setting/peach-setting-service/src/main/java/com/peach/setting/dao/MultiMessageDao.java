package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.MultiMessageDO;
import com.peach.setting.vo.MulitMessageVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 18:23
 */
@Indexed
@MybatisDao
public interface MultiMessageDao extends PeachDao<MultiMessageDO, MulitMessageVO> {

}
