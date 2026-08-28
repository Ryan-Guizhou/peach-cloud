package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.ResourceDTO;
import com.peach.auth.qo.ResourceQO;
import com.peach.auth.vo.ResourceVO;

import java.util.List;

/**
 * IResouce服务类。
 * <p>提供资源分页、查询以及基础增删改能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:34
 */
public interface IResouceService {

    /**
     * 分页查询资源列表。
     *
     * @param resourceQO 查询条件
     * @return 分页结果
     */
    PageInfo<ResourceVO> pageList(ResourceQO resourceQO);

    /**
     * 查询资源列表。
     *
     * @param resourceQO 查询条件
     * @return 资源列表
     */
    List<ResourceVO> list(ResourceQO resourceQO);

    /**
     * 根据ID查询资源。
     *
     * @param id 资源ID
     * @return 资源信息
     */
    ResourceVO selectById(String id);

    /**
     * 新增资源。
     *
     * @param resourceDTO 资源实体
     */
    void add(ResourceDTO resourceDTO);

    /**
     * 根据ID删除资源。
     *
     * @param id 资源ID
     */
    void delById(String id);

    /**
     * 更新资源。
     *
     * @param resourceDTO 资源实体
     */
    void update(ResourceDTO resourceDTO);
}
