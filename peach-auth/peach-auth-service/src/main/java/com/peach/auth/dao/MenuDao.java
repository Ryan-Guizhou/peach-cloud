package com.peach.auth.dao;

import com.peach.auth.qo.MenuQO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.MenuDO;
import com.peach.auth.vo.MenuVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.Collection;
import java.util.List;

/**
 * 菜单数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 16:59
 */
@Indexed
@MybatisDao
public interface MenuDao extends PeachDao<MenuDO, MenuVO> {

    List<MenuVO> selectByQO(MenuQO menuQO);

    List<MenuVO> selectByFuncCodes(@Param("tenantId") String tenantId,
                                   @Param("funcCodes") Collection<String> funcCodes);
    /** 查询同租户内可见目录，仅供补齐已授权菜单的父链。 */
    List<MenuVO> selectCatalogs(@Param("tenantId") String tenantId);
}
