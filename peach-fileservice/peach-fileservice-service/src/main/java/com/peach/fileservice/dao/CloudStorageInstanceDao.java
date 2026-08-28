package com.peach.fileservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.fileservice.entity.CloudStorageInstanceDO;
import com.peach.fileservice.qo.CloudStorageInstanceQO;
import com.peach.fileservice.vo.CloudStorageInstanceVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 云存储实例数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Indexed
@MybatisDao
public interface CloudStorageInstanceDao extends PeachDao<CloudStorageInstanceDO, CloudStorageInstanceVO> {

    /**
     * 根据查询对象条件查询云存储实例列表
     * 支持动态组合条件查询（如：实例名称、类型、状态、创建时间范围等）
     *
     * @param qo 查询条件对象，封装了所有可选的查询参数
     * @return 符合条件的云存储实例视图对象列表，若无匹配记录则返回空列表（非null）
     */
    List<CloudStorageInstanceVO> selectByQO(CloudStorageInstanceQO qo);

    /**
     * 查询所有已启用的云存储实例
     * 通常用于下拉选择、状态监控或定时任务等场景
     * 结果默认按创建时间降序或实例名称升序排列（具体取决于实现）
     *
     * @return 所有启用状态的云存储实例视图对象列表，若无数据则返回空列表（非null）
     */
    List<CloudStorageInstanceVO> selectAllEnabled();
}