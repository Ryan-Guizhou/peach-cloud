package com.peach.fileservice.service;

import com.peach.fileservice.dto.CloudStorageInstanceSaveDTO;
import com.peach.fileservice.qo.CloudStorageInstanceQO;
import com.peach.fileservice.vo.CloudStorageInstanceVO;

import java.util.List;

/**
 * ICloud存储实例服务类。
 * <p>
 * 负责管理系统中的云存储实例信息，包括：
 * 云存储实例新增、修改、删除、启停用、连接测试以及实例查询等功能。
 * </p>
 * <p>
 * 云存储实例主要用于维护不同类型存储服务的连接配置，
 * 例如对象存储（OSS、OBS、COS、S3）、文件存储（NAS）等。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9
 */
public interface ICloudStorageInstanceService {

    /**
     * 新增云存储实例。
     *
     * @param data 云存储实例保存参数
     * @return 新增后的云存储实例信息
     */
    CloudStorageInstanceVO add(CloudStorageInstanceSaveDTO data);

    /**
     * 更新云存储实例。
     *
     * @param data 云存储实例更新参数
     * @return 更新后的云存储实例信息
     */
    CloudStorageInstanceVO update(CloudStorageInstanceSaveDTO data);

    /**
     * 删除云存储实例。
     *
     * <p>
     * 删除操作通常需要校验当前实例是否仍被业务文件引用，
     * 避免删除正在使用的存储配置。
     * </p>
     *
     * @param instanceId 云存储实例ID
     */
    void delete(String instanceId);

    /**
     * 启用云存储实例。
     *
     * <p>
     * 启用后，该存储实例可以被业务模块选择和使用。
     * </p>
     *
     * @param instanceId 云存储实例ID
     */
    void enable(String instanceId);

    /**
     * 禁用云存储实例。
     *
     * <p>
     * 禁用后，该存储实例不可用于新的文件操作，
     * 但历史文件数据是否允许访问需要根据业务策略决定。
     * </p>
     *
     * @param instanceId 云存储实例ID
     */
    void disable(String instanceId);

    /**
     * 测试云存储连接。
     *
     * <p>
     * 根据提供的存储配置创建临时存储客户端，
     * 验证目标存储服务是否可正常访问。
     * </p>
     *
     * @param data 云存储实例连接配置参数
     * @return true表示连接成功，false表示连接失败
     */
    boolean testConnection(CloudStorageInstanceSaveDTO data);

    /**
     * 根据实例ID查询云存储实例。
     *
     * @param instanceId 云存储实例ID
     * @return 云存储实例信息
     */
    CloudStorageInstanceVO selectById(String instanceId);

    /**
     * 查询云存储实例列表。
     *
     * @param data 查询条件参数
     * @return 云存储实例列表
     */
    List<CloudStorageInstanceVO> list(CloudStorageInstanceQO data);

    /**
     * 查询已启用的云存储实例列表。
     *
     * <p>
     * 主要用于业务模块初始化存储客户端、
     * 存储选择以及运行时加载可用存储配置。
     * </p>
     *
     * @return 已启用的云存储实例列表
     */
    List<CloudStorageInstanceVO> listEnabled();

}