package com.peach.storage.spi.support;

import com.peach.config.StorageProperties;
import com.peach.content.UploadContent;
import com.peach.enums.StorageCapability;
import com.peach.enums.StorageResultCode;
import com.peach.exception.StorageException;
import com.peach.request.BatchDeleteObjectsRequest;
import com.peach.request.CopyObjectRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.MoveObjectRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.CopyResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.MoveResult;
import com.peach.response.ObjectInfo;
import com.peach.storage.spi.StorageProvider;
import com.peach.util.StoragePathUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link StorageProvider} SPI 支撑工具类。
 *
 * <p>为存储服务提供者（StorageProvider）的 SPI 实现提供通用能力封装，
 * 包括对象 Key 处理、列表结果构建、拷贝/移动等复合操作的模板方法。
 * 旨在减少各存储实现（如 OSS、S3、本地文件等）中的重复代码。
 * </p>
 *
 * <p><b>核心功能：</b>
 * <ul>
 *   <li>对象 Key 规范化与业务前缀剥离</li>
 *   <li>Endpoint 标准化（自动补全协议和 Host 提取）</li>
 *   <li>递归拷贝与移动的复杂逻辑封装</li>
 *   <li>列表结果构建和基础能力集声明</li>
 * </ul>
 * </p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 18:02
 */
public final class StorageProviderSupport {

    private StorageProviderSupport() {
        // 私有构造函数，防止实例化
    }

    /**
     * 将 {@link Date} 转换为 {@link Instant}。
     *
     * <p>用于将存储服务返回的传统 Date 对象转换为 Java 8+ 时间 API。</p>
     *
     * @param date 待转换的 Date 对象，可为 null
     * @return 转换后的 Instant 对象，若输入为 null 则返回 null
     */
    public static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    /**
     * 规范化 Endpoint 地址。
     *
     * <p>自动为缺少协议的地址补全 {@code https://} 前缀，并去除首尾空白。
     * 适用于将用户配置的简化地址（如 {@code oss-cn-hangzhou.aliyuncs.com}）
     * 转换为标准 URI 格式。</p>
     *
     * @param endpoint 原始 Endpoint 字符串，可为 null
     * @return 规范化后的 Endpoint URI 字符串，若输入为 null 或空白则返回原值
     * @throws IllegalArgumentException 如果 Endpoint 不是合法的 URI 格式
     */
    public static String normalizeEndpoint(String endpoint) {
        String value = endpoint == null ? null : endpoint.trim();
        if (value == null || value.isEmpty()) {
            return value;
        }
        URI uri = URI.create(value.contains("://") ? value : "https://" + value);
        return uri.toString();
    }

    /**
     * 从 Endpoint 中提取主机名（Host）。
     *
     * <p>用于签名计算或网络连接时获取服务端域名/IP。</p>
     *
     * @param endpoint Endpoint 字符串，可为 null
     * @return 提取的主机名，若输入为 null 或解析失败则返回原值
     */
    public static String resolveEndpointHost(String endpoint) {
        String value = endpoint == null ? null : endpoint.trim();
        if (value == null || value.isEmpty()) {
            return null;
        }
        URI uri = URI.create(value.contains("://") ? value : "https://" + value);
        return uri.getHost() == null ? value : uri.getHost();
    }

    /**
     * 从完整对象 Key 中剥离业务前缀，还原业务原始 Key。
     *
     * <p>假设配置的前缀为 {@code "prod/images"}，完整 Key 为 {@code "prod/images/user/avatar.jpg"}，
     * 则返回 {@code "user/avatar.jpg"}。若完整 Key 与配置前缀完全相同，返回空字符串。</p>
     *
     * <p><b>使用场景：</b>存储服务返回的 Key 包含全局前缀，但业务层只关心相对路径。</p>
     *
     * @param config 存储提供者配置，可为 null
     * @param actualObjectKey 实际存储的完整对象 Key
     * @return 剥离前缀后的业务 Key，若 actualObjectKey 为 null 则返回 null
     */
    public static String businessObjectKey(StorageProperties.StorageProvider config, String actualObjectKey) {
        if (actualObjectKey == null) {
            return null;
        }
        String prefix = config == null ? null : config.getPrefix();
        if (StringUtils.isBlank(prefix)) {
            return actualObjectKey;
        }
        String normalizedPrefix = StoragePathUtil.normalizeObjectKey(prefix);
        if (actualObjectKey.equals(normalizedPrefix)) {
            return ""; // Key 恰好等于前缀本身，视为根目录
        }
        String expectedPrefix = normalizedPrefix + "/";
        if (actualObjectKey.startsWith(expectedPrefix)) {
            return actualObjectKey.substring(expectedPrefix.length());
        }
        return actualObjectKey;
    }

    /**
     * 构建标准化的对象列表结果。
     *
     * <p>为各存储提供者的列表操作提供统一的结果对象构建入口。</p>
     *
     * @param providerName 提供者名称
     * @param bucketName 存储桶/容器名称
     * @param prefix 查询前缀
     * @param items 对象信息列表
     * @param nextContinuationToken 分页续标令牌
     * @param truncated 是否还有更多数据
     * @param commonPrefixes 公共前缀列表（模拟目录层级）
     * @return 标准化的列表结果对象
     */
    public static ListObjectsResult buildListResult(String providerName, String bucketName, String prefix,
                                                    List<ObjectInfo> items, String nextContinuationToken,
                                                    boolean truncated, List<String> commonPrefixes) {
        return ListObjectsResult.builder()
                .providerName(providerName)
                .bucketName(bucketName)
                .prefix(prefix)
                .items(items)
                .nextContinuationToken(nextContinuationToken)
                .truncated(truncated)
                .commonPrefixes(commonPrefixes)
                .build();
    }

    /**
     * 声明存储提供者的基础能力集。
     *
     * <p>所有存储提供者默认支持：拷贝、移动、批量删除、存储桶存在性检查、
     * 预签名获取 URL、自定义域名。根据是否支持公共读权限，额外添加 {@link StorageCapability#PUBLIC_READ_ACL}。</p>
     *
     * @param supportsPublicRead 是否支持公共读权限
     * @return 基础能力集（不可变 Set）
     */
    public static Set<StorageCapability> baseCapabilities(boolean supportsPublicRead) {
        EnumSet<StorageCapability> capabilities = EnumSet.of(
                StorageCapability.COPY,
                StorageCapability.MOVE,
                StorageCapability.BATCH_DELETE,
                StorageCapability.BUCKET_EXISTS,
                StorageCapability.PRESIGNED_GET_URL,
                StorageCapability.CUSTOM_DOMAIN
        );
        if (supportsPublicRead) {
            capabilities.add(StorageCapability.PUBLIC_READ_ACL);
        }
        return capabilities;
    }

    /**
     * 返回空的公共前缀列表。
     *
     * <p>用于列表结果中 {@code commonPrefixes} 字段的默认值。</p>
     *
     * @return 空的 {@link ArrayList} 实例
     */
    public static List<String> noCommonPrefixes() {
        return new ArrayList<String>();
    }

    /**
     * 执行对象拷贝操作（支持单个或递归拷贝）。
     *
     * <p><b>递归拷贝逻辑：</b>
     * <ol>
     *   <li>递归列举源路径下的所有对象</li>
     *   <li>验证目标路径不能是源路径的子路径（防止死循环）</li>
     *   <li>逐个拷贝对象，保留相对路径结构</li>
     * </ol>
     * </p>
     *
     * <p><b>示例：</b>
     * <pre>
     * 源路径: "folder/"（包含 a.txt, sub/b.txt）
     * 目标路径: "backup/"
     * 结果: "backup/a.txt", "backup/sub/b.txt"
     * </pre>
     * </p>
     *
     * @param provider 存储提供者实例
     * @param request 拷贝请求参数
     * @return 拷贝结果对象
     * @throws StorageException 当源对象不存在、目标已存在且未允许覆盖、或发生其他存储错误时抛出
     */
    public static CopyResult copy(StorageProvider provider, CopyObjectRequest request) {
        if (request == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Copy request must not be null");
        }
        try {
            // 非递归拷贝：直接拷贝单个对象
            if (!request.isRecursive()) {
                copySingleObject(provider, request.getSourceBucketName(), request.getSourceObjectKey(),
                        request.getTargetBucketName(), request.getTargetObjectKey(), request.isOverwrite());
                return new CopyResult(provider.name(), request.getSourceBucketName(), request.getSourceObjectKey(),
                        request.getTargetBucketName(), request.getTargetObjectKey(), true);
            }

            // 递归拷贝：列举所有源对象
            LinkedHashSet<String> sourceKeys = listRecursiveSourceKeys(
                    provider, request.getSourceBucketName(), request.getSourceObjectKey());
            if (sourceKeys.isEmpty()) {
                throw new StorageException(StorageResultCode.OBJECT_NOT_FOUND,
                        "Object not found: " + request.getSourceObjectKey());
            }

            // 规范化源路径和目标路径（确保以 / 结尾）
            String sourcePrefix = normalizeDirectoryPrefix(provider, request.getSourceObjectKey());
            String targetPrefix = normalizeDirectoryPrefix(provider, request.getTargetObjectKey());

            // 安全校验：防止递归拷贝陷入死循环
            validateRecursiveTarget(sourcePrefix, targetPrefix);

            // 逐个拷贝，保持相对路径结构
            for (String sourceKey : sourceKeys) {
                String targetKey = resolveTargetKey(provider, sourceKey, sourcePrefix, targetPrefix);
                copySingleObject(provider, request.getSourceBucketName(), sourceKey,
                        request.getTargetBucketName(), targetKey, request.isOverwrite());
            }
            return new CopyResult(provider.name(), request.getSourceBucketName(), request.getSourceObjectKey(),
                    request.getTargetBucketName(), request.getTargetObjectKey(), true);
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to " + "copy object: " + request.getSourceObjectKey(), ex);
        }
    }

    /**
     * 执行对象移动操作（支持单个或递归移动）。
     *
     * <p>实现原理：先执行 {@link #copy(StorageProvider, CopyObjectRequest)}，
     * 拷贝成功后删除源对象。若拷贝过程中发生异常，源对象不受影响（保证原子性）。
     * 递归移动时，批量删除所有源对象。</p>
     *
     * @param provider 存储提供者实例
     * @param request 移动请求参数
     * @return 移动结果对象
     * @throws StorageException 当源对象不存在或移动过程中发生错误时抛出
     */
    public static MoveResult move(StorageProvider provider, MoveObjectRequest request) {
        if (request == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Move request must not be null");
        }
        try {
            // 第一步：执行拷贝
            copy(provider, CopyObjectRequest.builder()
                    .sourceBucketName(request.getSourceBucketName())
                    .sourceObjectKey(request.getSourceObjectKey())
                    .targetBucketName(request.getTargetBucketName())
                    .targetObjectKey(request.getTargetObjectKey())
                    .recursive(request.isRecursive())
                    .overwrite(request.isOverwrite())
                    .build());

            // 第二步：删除源对象
            if (!request.isRecursive()) {
                // 单个删除
                provider.delete(DeleteObjectRequest.builder()
                        .bucketName(request.getSourceBucketName())
                        .objectKey(request.getSourceObjectKey())
                        .build());
            } else {
                // 批量删除（递归模式）
                LinkedHashSet<String> sourceKeys = listRecursiveSourceKeys(
                        provider, request.getSourceBucketName(), request.getSourceObjectKey());
                provider.batchDelete(BatchDeleteObjectsRequest.builder()
                        .bucketName(request.getSourceBucketName())
                        .objectKeys(new ArrayList<String>(sourceKeys))
                        .build());
            }
            return new MoveResult(provider.name(), request.getSourceBucketName(), request.getSourceObjectKey(),
                    request.getTargetBucketName(), request.getTargetObjectKey(), true);
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StorageException(StorageResultCode.STORAGE_ERROR,
                    "Failed to " + "move object: " + request.getSourceObjectKey(), ex);
        }
    }


    /**
     * 拷贝单个对象（底层实现）。
     *
     * <p>流程：
     * <ol>
     *   <li>检查源对象是否存在，不存在则抛出 {@link StorageResultCode#OBJECT_NOT_FOUND}</li>
     *   <li>若不允许覆盖且目标已存在，抛出异常</li>
     *   <li>获取源对象的元数据（大小、类型、自定义属性）</li>
     *   <li>下载源对象内容并上传至目标位置</li>
     * </ol>
     * </p>
     *
     * @param provider 存储提供者实例
     * @param sourceBucketName 源存储桶名称
     * @param sourceObjectKey 源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetObjectKey 目标对象 Key
     * @param overwrite 是否允许覆盖已存在的目标对象
     * @throws Exception 任何 I/O 或存储错误
     */
    private static void copySingleObject(StorageProvider provider, String sourceBucketName, String sourceObjectKey,
                                         String targetBucketName, String targetObjectKey,
                                         boolean overwrite) throws Exception {
        // 1. 源对象存在性检查
        if (!provider.exists(sourceBucketName, sourceObjectKey)) {
            throw new StorageException(StorageResultCode.OBJECT_NOT_FOUND,
                    "Object not found: " + sourceObjectKey);
        }

        // 2. 目标对象覆盖检查
        if (!overwrite && provider.exists(targetBucketName, targetObjectKey)) {
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Target object already exists: " + targetObjectKey);
        }

        // 3. 获取源对象元数据（用于保持 Content-Type 和自定义属性）
        ObjectInfo sourceInfo = provider.stat(DownloadObjectRequest.builder()
                .bucketName(sourceBucketName)
                .objectKey(sourceObjectKey)
                .build());

        // 4. 流式下载并上传（使用 try-with-resources 确保流关闭）
        try (InputStream inputStream = provider.download(DownloadObjectRequest.builder()
                .bucketName(sourceBucketName)
                .objectKey(sourceObjectKey)
                .build())) {
            provider.upload(UploadObjectRequest.builder()
                    .bucketName(targetBucketName)
                    .objectKey(targetObjectKey)
                    .content(UploadContent.of(inputStream, sourceInfo.getSize()))
                    .contentType(sourceInfo.getContentType())
                    .metadata(sourceInfo.getMetadata())
                    .build());
        }
    }

    /**
     * 递归列举指定路径下的所有对象 Key。
     *
     * <p>使用分页方式（每页 1000 条）遍历所有对象，避免一次性加载过多数据导致 OOM。</p>
     *
     * <p><b>注意：</b>如果源路径指向一个具体文件（非目录），则只返回该文件自身的 Key。</p>
     *
     * @param provider 存储提供者实例
     * @param bucketName 存储桶名称
     * @param sourceObjectKey 源对象路径（可以是文件或目录）
     * @return 所有对象 Key 的 {@link LinkedHashSet}（保持列举顺序）
     */
    private static LinkedHashSet<String> listRecursiveSourceKeys(StorageProvider provider, String bucketName,
                                                                 String sourceObjectKey) {
        LinkedHashSet<String> sourceKeys = new LinkedHashSet<String>();

        // 如果直接命中一个文件，直接返回
        if (provider.exists(bucketName, sourceObjectKey)) {
            sourceKeys.add(sourceObjectKey);
        }

        // 以 / 结尾作为目录前缀进行递归列举
        String prefix = normalizeDirectoryPrefix(provider, sourceObjectKey);
        String continuationToken = null;
        do {
            ListObjectsResult result = provider.list(ListObjectsRequest.builder()
                    .bucketName(bucketName)
                    .prefix(prefix)
                    .recursive(true) // 递归模式，直接返回所有子对象
                    .maxKeys(1000)   // 分页大小
                    .continuationToken(continuationToken)
                    .build());

            if (result.getItems() != null) {
                for (ObjectInfo item : result.getItems()) {
                    if (item != null && item.getObjectKey() != null) {
                        sourceKeys.add(item.getObjectKey());
                    }
                }
            }
            continuationToken = result.getNextContinuationToken();
        } while (continuationToken != null && !continuationToken.isBlank());

        return sourceKeys;
    }

    /**
     * 将对象 Key 规范化为目录前缀（确保以 / 结尾）。
     *
     * <p>用于递归操作的源/目标路径规范化。</p>
     *
     * @param provider 存储提供者实例（用于获取原始 Key 格式）
     * @param objectKey 对象 Key
     * @return 以 / 结尾的目录前缀，若输入为空则返回空字符串
     */
    private static String normalizeDirectoryPrefix(StorageProvider provider, String objectKey) {
        String normalized = provider.rawObjectKey(objectKey);
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    /**
     * 验证递归拷贝/移动的目标路径是否合法。
     *
     * <p>校验规则：
     * <ol>
     *   <li>源路径和目标路径不能完全相同</li>
     *   <li>目标路径不能是源路径的子路径（防止递归拷贝导致无限循环）</li>
     * </ol>
     * </p>
     *
     * <p><b>示例：</b>
     * <pre>
     * 源: "folder/", 目标: "backup/"    -> 通过
     * 源: "folder/", 目标: "folder/"    -> 抛出异常（完全相同）
     * 源: "folder/", 目标: "folder/sub/" -> 抛出异常（目标在源内部）
     * </pre>
     * </p>
     *
     * @param sourcePrefix 源路径前缀（以 / 结尾）
     * @param targetPrefix 目标路径前缀（以 / 结尾）
     * @throws StorageException 当目标路径不合法时抛出
     */
    private static void validateRecursiveTarget(String sourcePrefix, String targetPrefix) {
        if (sourcePrefix.equals(targetPrefix)) {
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Source object key and target object key must not be the same");
        }
        if (!sourcePrefix.isEmpty() && targetPrefix.startsWith(sourcePrefix)) {
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Target object must not be inside source path: " + targetPrefix);
        }
    }

    /**
     * 根据源对象 Key 和路径映射关系，计算目标对象 Key。
     *
     * <p><b>转换逻辑：</b>
     * <pre>
     * 源前缀: "src/"
     * 目标前缀: "dst/"
     * 源 Key: "src/sub/file.txt"
     * 计算结果: "dst/sub/file.txt"  （保留相对路径结构）
     * </pre>
     * </p>
     *
     * <p>特殊处理：如果源 Key 恰好等于源目录前缀（去掉尾部 /），
     * 则目标 Key 直接为目标目录前缀（去掉尾部 /）。</p>
     *
     * @param provider 存储提供者实例
     * @param sourceKey 源对象 Key
     * @param sourcePrefix 源目录前缀（以 / 结尾）
     * @param targetPrefix 目标目录前缀（以 / 结尾）
     * @return 计算后的目标对象 Key
     * @throws StorageException 当源 Key 不在源前缀范围内时抛出
     */
    private static String resolveTargetKey(StorageProvider provider, String sourceKey, String sourcePrefix,
                                           String targetPrefix) {
        // 特殊：源 Key 本身就是目录本身（如 "folder" 匹配源前缀 "folder/"）
        if (sourceKey.equals(removeTrailingSlash(sourcePrefix))) {
            return removeTrailingSlash(targetPrefix);
        }

        String normalizedSourceKey = provider.rawObjectKey(sourceKey);
        if (!normalizedSourceKey.startsWith(sourcePrefix)) {
            throw new StorageException(StorageResultCode.BAD_REQUEST,
                    "Source object key is outside recursive prefix: " + sourceKey);
        }

        // 剥离源前缀，附加目标前缀
        String relativeKey = normalizedSourceKey.substring(sourcePrefix.length());
        return targetPrefix + relativeKey;
    }

    /**
     * 移除字符串末尾的斜杠（仅处理 /）。
     *
     * <p>用于目录路径与对象 Key 的等价转换。</p>
     *
     * @param value 待处理的字符串
     * @return 移除末尾斜杠后的字符串，若输入为 null 或空则返回原值
     */
    private static String removeTrailingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
