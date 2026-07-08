package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.OrganizationDTO;
import com.peach.auth.qo.OrganizationQO;
import com.peach.auth.vo.OrganizationVO;

import java.util.List;

/**
 * 机构服务接口。
 *
 * <p>提供机构的分页查询、单条查询和基础增删改能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
public interface IOrganizationService {

    /**
     * 分页查询机构列表。
     *
     * @param organizationQO 查询条件，包含租户、机构编码、机构名称等过滤字段
     * @return 机构分页结果
     */
    PageInfo<OrganizationVO> pageList(OrganizationQO organizationQO);

    /**
     * 查询机构列表。
     *
     * @param organizationQO 查询条件，支持按租户和机构条件过滤
     * @return 机构列表
     */
    List<OrganizationVO> list(OrganizationQO organizationQO);

    /**
     * 根据机构ID查询机构详情。
     *
     * @param id 机构ID
     * @return 机构详情
     */
    OrganizationVO selectById(String id);

    /**
     * 新增机构。
     *
     * @param organizationDTO 机构新增参数
     */
    void add(OrganizationDTO organizationDTO);

    /**
     * 根据机构ID删除机构。
     *
     * @param id 机构ID
     */
    void delById(String id);

    /**
     * 更新机构。
     *
     * @param organizationDTO 机构更新参数
     */
    void update(OrganizationDTO organizationDTO);
}
