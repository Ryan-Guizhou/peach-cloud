package com.peach.auth.dao;

import com.peach.auth.qo.MenuQO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.MenuDO;
import com.peach.auth.vo.MenuVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 16:59
 */
@Indexed
@MybatisDao
public interface MenuDao extends PeachDao<MenuDO, MenuVO> {

    List<MenuVO> selectByQO(MenuQO menuQO);
}
