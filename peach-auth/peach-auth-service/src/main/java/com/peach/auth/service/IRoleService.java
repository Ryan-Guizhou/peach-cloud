package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.RoleDTO;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.vo.RoleVO;

import java.util.List;

/**
 * IRole服务类。
 * <p>提供角色分页、查询以及基础增删改能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:25
 */
public interface IRoleService {

    /**
     * 分页查询角色列表。
     *
     * @param roleQO 查询条件
     * @return 分页结果
     */
    PageInfo<RoleVO> pageList(RoleQO roleQO);

    /**
     * 查询角色列表。
     *
     * @param roleQO 查询条件
     * @return 角色列表
     */
    List<RoleVO> list(RoleQO roleQO);

    /**
     * 根据ID查询角色。
     *
     * @param id 角色ID
     * @return 角色信息
     */
    RoleVO selectById(String id);

    /**
     * 新增角色。
     *
     * @param roleDTO 角色入参
     */
    void add(RoleDTO roleDTO);

    /**
     * 根据ID删除角色。
     *
     * @param id 角色ID
     */
    void delById(String id);

    /**
     * 更新角色。
     *
     * @param roleDTO 角色入参
     */
    void update(RoleDTO roleDTO);

    /**
     * 根据用户与年度查询角色。
     *
     * @param roleQO 查询条件
     * @return 角色列表
     */
    List<RoleVO> selectByUserCode(RoleQO roleQO);
}
