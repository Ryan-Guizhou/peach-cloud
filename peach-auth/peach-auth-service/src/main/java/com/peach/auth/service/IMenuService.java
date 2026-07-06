package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.MenuDTO;
import com.peach.auth.entity.MenuDO;
import com.peach.auth.qo.MenuQO;
import com.peach.auth.vo.MenuVO;

import java.util.List;

/**
 * 菜单服务接口。
 *
 * <p>提供菜单分页、查询以及基础增删改能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:25
 */
public interface IMenuService {

    /**
     * 分页查询菜单列表。
     *
     * @param menuQO 查询条件
     * @return 分页结果
     */
    PageInfo<MenuVO> pageList(MenuQO menuQO);

    /**
     * 根据ID查询菜单。
     *
     * @param id 菜单ID
     * @return 菜单信息
     */
    MenuVO selectById(String id);

    /**
     * 新增菜单。
     *
     * @param menuDTO 菜单实体
     */
    void add(MenuDTO menuDTO);

    /**
     * 根据ID删除菜单。
     *
     * @param id 菜单ID
     */
    void delById(String id);

    /**
     * 更新菜单。
     *
     * @param menuDTO 菜单实体
     */
    void update(MenuDTO menuDTO);
}
