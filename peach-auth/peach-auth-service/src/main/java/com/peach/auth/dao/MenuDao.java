package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.MenuDO;
import com.peach.auth.vo.MenuVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 16:59
 */
@Indexed
@MybatisDao
public interface MenuDao extends PeachDao<MenuDO, MenuVO> {
}
