package com.peach.fileservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.peach.common.util.StringUtil;
import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptFactory;
import com.peach.common.util.encrypt.EncryptService;
import com.peach.config.StorageProperties;
import com.peach.enums.StorageType;
import com.peach.fileservice.entity.CloudStorageInstanceDO;
import com.peach.fileservice.vo.CloudStorageInstanceVO;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 云存储实例公共辅助组件。
 *
 * <p>
 * 用于处理云存储实例相关的公共转换及辅助逻辑，包括：
 * </p>
 *
 * <ul>
 *     <li>数据库实体转换为存储运行时配置</li>
 *     <li>数据库实体转换为接口返回对象</li>
 *     <li>存储密钥加密、解密及脱敏处理</li>
 *     <li>当前操作用户信息获取</li>
 *     <li>扩展配置解析</li>
 * </ul>
 *
 * <p>
 * 该组件主要用于抽取云存储实例管理过程中重复代码，
 * 简化 Service 层业务逻辑。
 * </p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/7/9
 */
@Component
public class CloudStorageInstanceSupport {

    /**
     * 将云存储实例数据库对象转换为运行时存储配置。
     *
     * <p>
     * 该方法用于根据数据库中保存的存储实例信息，
     * 构建存储客户端所需的运行时配置对象。
     * </p>
     *
     * <p>
     * 转换过程中会处理：
     * </p>
     *
     * <ul>
     *     <li>存储类型转换</li>
     *     <li>SecretKey 解密</li>
     *     <li>扩展参数解析</li>
     *     <li>布尔类型配置转换</li>
     * </ul>
     *
     * @param instanceDO 云存储实例数据库对象
     * @return 存储提供方运行时配置
     */
    public StorageProperties.StorageProvider toProviderConfig(CloudStorageInstanceDO instanceDO) {
        StorageProperties.StorageProvider provider = new StorageProperties.StorageProvider();
        provider.setName(instanceDO.getInstanceId());
        provider.setType(StorageType.parse(instanceDO.getStoreType()));
        provider.setEndpoint(instanceDO.getEndpoint());
        provider.setRegion(instanceDO.getRegion());
        provider.setBucketName(instanceDO.getBucketName());
        provider.setPrefix(instanceDO.getPrefix());
        provider.setAccessKey(instanceDO.getAccessKey());
        provider.setSecretKey(decryptIfNecessary(instanceDO.getSecretKey()));
        provider.setRootPath(instanceDO.getRootPath());
        provider.setDomain(instanceDO.getDomain());
        provider.setPathStyleAccess(Integer.valueOf(1).equals(instanceDO.getPathStyleAccess()));
        provider.setPublicRead(Integer.valueOf(1).equals(instanceDO.getPublicRead()));
        provider.setExtraProperties(parseExtra(instanceDO.getExtraJson()));
        return provider;
    }

    /**
     * 将云存储实例数据库对象转换为展示对象。
     *
     * <p>
     * 用于接口返回场景，将数据库实体转换为前端展示所需 VO。
     * </p>
     *
     * <p>
     * 返回过程中会生成 SecretKey 脱敏字段，
     * 避免敏感信息完整暴露。
     * </p>
     *
     * @param instanceDO 云存储实例数据库对象
     * @return 云存储实例展示对象
     */
    public CloudStorageInstanceVO toView(CloudStorageInstanceDO instanceDO) {
        CloudStorageInstanceVO result = new CloudStorageInstanceVO();
        result.setInstanceId(instanceDO.getInstanceId());
        result.setInstanceName(instanceDO.getInstanceName());
        result.setStoreType(instanceDO.getStoreType());
        result.setEndpoint(instanceDO.getEndpoint());
        result.setRegion(instanceDO.getRegion());
        result.setBucketName(instanceDO.getBucketName());
        result.setPrefix(instanceDO.getPrefix());
        result.setAccessKey(instanceDO.getAccessKey());
        result.setSecretKey(instanceDO.getSecretKey());
        result.setSecretKeyMasked(maskSecret(instanceDO.getSecretKey()));
        result.setRootPath(instanceDO.getRootPath());
        result.setDomain(instanceDO.getDomain());
        result.setPathStyleAccess(instanceDO.getPathStyleAccess());
        result.setPublicRead(instanceDO.getPublicRead());
        result.setExtraJson(instanceDO.getExtraJson());
        result.setEnabled(instanceDO.getEnabled());
        result.setRemark(instanceDO.getRemark());
        result.setCreatedTime(instanceDO.getCreatedTime());
        result.setCreatorId(instanceDO.getCreatorId());
        result.setModifyTime(instanceDO.getModifyTime());
        result.setModifierId(instanceDO.getModifierId());
        return result;
    }

    /**
     * 加密存储密钥。
     *
     * <p>
     * 云存储访问密钥属于敏感信息，
     * 保存数据库前需要进行加密处理。
     * </p>
     *
     * @param secretKey 明文密钥
     * @return 加密后的密钥
     */
    public String encryptSecret(String secretKey) {
        if (StringUtil.isBlank(secretKey)) {
            return secretKey;
        }
        try {
            EncryptService encryptService = EncryptFactory.getEncrypt(EncryptConst.AES);
            return encryptService.encrypt(secretKey);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to encrypt secret key", ex);
        }
    }

    /**
     * 获取当前操作用户 ID。
     *
     * <p>
     * 用于记录云存储实例创建、修改等操作的操作人信息。
     * 当当前请求不存在用户上下文时，返回系统默认用户。
     * </p>
     *
     * @return 当前操作用户 ID
     */
    public String currentOperator() {
        UserContext context = SecurityContextHolder.get();
        if (context == null || StringUtil.isBlank(context.getUserId())) {
            return "system";
        }
        return context.getUserId();
    }

    /**
     * 必要时解密存储密钥。
     *
     * <p>
     * 如果密钥为空，则直接返回；
     * 如果解密失败，则认为当前数据可能为历史明文数据，
     * 直接返回原始值，保证历史数据兼容。
     * </p>
     *
     * @param secretKey 存储密钥
     * @return 解密后的密钥
     */
    private String decryptIfNecessary(String secretKey) {
        if (StringUtil.isBlank(secretKey)) {
            return secretKey;
        }
        try {
            EncryptService encryptService = EncryptFactory.getEncrypt(EncryptConst.AES);
            return encryptService.decrypt(secretKey);
        } catch (Exception ex) {
            return secretKey;
        }
    }

    /**
     * 解析扩展配置。
     *
     * <p>
     * 不同类型存储可能存在额外配置参数，
     * 统一通过 JSON 保存并转换为 Map 结构。
     * </p>
     *
     * @param extraJson 扩展配置 JSON 字符串
     * @return 扩展配置集合
     */
    private Map<String, String> parseExtra(String extraJson) {
        if (StringUtil.isBlank(extraJson)) {
            return Collections.emptyMap();
        }
        return JSON.parseObject(extraJson, new TypeReference<Map<String, String>>() {
        });
    }

    /**
     * 对敏感密钥进行脱敏处理。
     *
     * <p>
     * 保留密钥末尾部分字符用于页面展示，
     * 避免完整密钥泄露。
     * </p>
     *
     * @param secretKey 原始密钥
     * @return 脱敏后的密钥
     */
    private String maskSecret(String secretKey) {
        if (StringUtil.isBlank(secretKey)) {
            return null;
        }
        int visible = Math.min(4, secretKey.length());
        String suffix = secretKey.substring(secretKey.length() - visible);
        return "****" + suffix;
    }
}
