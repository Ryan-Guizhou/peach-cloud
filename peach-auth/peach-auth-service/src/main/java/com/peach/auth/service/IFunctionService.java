package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.FunctionDTO;
import com.peach.auth.qo.FunctionQO;
import com.peach.auth.vo.FunctionVO;

import java.util.List;

/**
 * IFunction服务类。
 * <p>提供功能分页、查询以及基础增删改能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:25
 */
public interface IFunctionService {

    /**
     * 分页查询功能列表。
     *
     * @param functionQO 查询条件
     * @return 分页结果
     */
    PageInfo<FunctionVO> pageList(FunctionQO functionQO);

    /**
     * 查询功能列表。
     *
     * @param functionQO 查询条件
     * @return 功能列表
     */
    List<FunctionVO> list(FunctionQO functionQO);

    /**
     * 根据ID查询功能。
     *
     * @param id 功能ID
     * @return 功能信息
     */
    FunctionVO selectById(String id);

    /**
     * 新增功能。
     *
     * @param functionDTO 功能实体
     */
    void add(FunctionDTO functionDTO);

    /**
     * 根据ID删除功能。
     *
     * @param id 功能ID
     */
    void delById(String id);

    /**
     * 更新功能。
     *
     * @param functionDTO 功能实体
     */
    void update(FunctionDTO functionDTO);
}
