package com.peach.manager.impl;

import com.peach.config.StorageProperties;
import com.peach.content.UploadContent;
import com.peach.enums.StorageType;
import com.peach.manager.CloudStorageManagerService;
import com.peach.manager.support.RuntimeStorageProviderFactory;
import com.peach.request.BatchDeleteObjectsRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.DeleteResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.response.UploadResult;
import com.peach.storage.spi.StorageProvider;
import com.peach.util.StoragePathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 默认的云存储管理器服务实现（面向浏览器）
 * <p>
 * 该类是云存储操作的核心实现，提供了统一的存储访问接口，支持：
 * <ul>
 *   <li><b>多存储类型支持</b>：本地文件系统（LOCAL）、网络附加存储（NAS）、以及各类云存储（OSS、COS、S3等）</li>
 *   <li><b>目录模拟机制</b>：通过占位符文件（.peach-dir）在云存储中模拟空目录</li>
 *   <li><b>路径规范化</b>：统一使用正斜杠（/）作为路径分隔符，兼容不同操作系统</li>
 *   <li><b>分页查询</b>：支持递归和非递归两种列表模式，并支持基于令牌的分页</li>
 *   <li><b>资源管理</b>：通过 try-with-resources 确保存储提供者资源的正确释放</li>
 * </ul>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
public class DefaultCloudStorageManagerService implements CloudStorageManagerService {


    public static final String DIRECTORY_PLACEHOLDER_FILE = ".peach-dir";

    private final RuntimeStorageProviderFactory runtimeStorageProviderFactory;


    public DefaultCloudStorageManagerService(RuntimeStorageProviderFactory runtimeStorageProviderFactory) {
        this.runtimeStorageProviderFactory = runtimeStorageProviderFactory;
    }


    /**
     * 测试存储连接是否正常
     * <p>
     * 通过检查存储桶是否存在来验证连接的有效性
     *
     * @param providerConfig 存储提供者配置
     * @return true-连接正常；false-连接异常
     */
    @Override
    public boolean testConnection(StorageProperties.StorageProvider providerConfig) {
        return bucketExists(providerConfig);
    }

    /**
     * 检查存储桶是否存在
     *
     * @param providerConfig 存储提供者配置
     * @return true-存储桶存在；false-不存在
     * @throws RuntimeException 检查过程中发生异常时抛出
     */
    @Override
    public boolean bucketExists(StorageProperties.StorageProvider providerConfig) {
        try (StorageProvider storageProvider = runtimeStorageProviderFactory.create(providerConfig)) {
            return storageProvider.bucketExists(storageProvider.bucketName(providerConfig));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to test storage bucket", ex);
        }
    }

    /**
     * 检查指定对象是否存在
     *
     * @param providerConfig 存储提供者配置
     * @param objectKey      对象键（相对于存储桶的完整路径）
     * @return true-对象存在；false-不存在
     * @throws RuntimeException 检查过程中发生异常时抛出
     */
    @Override
    public boolean objectExists(StorageProperties.StorageProvider providerConfig, String objectKey) {
        try (StorageProvider storageProvider = runtimeStorageProviderFactory.create(providerConfig)) {
            return storageProvider.exists(normalizeRelativePath(objectKey));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to check object exists", ex);
        }
    }

    /**
     * 列出存储对象列表
     * <p>
     * 根据存储类型采用不同的处理策略：
     * <ul>
     *   <li><b>本地存储（LOCAL/NAS）</b>：直接操作文件系统，利用 Files API 进行目录遍历</li>
     *   <li><b>云存储</b>：调用 StorageProvider.list() 方法，并对返回结果进行占位符过滤</li>
     * </ul>
     *
     * @param providerConfig 存储提供者配置
     * @param request        列表查询请求（包含前缀、递归标志、分页参数等）
     * @return 列表查询结果（包含对象列表、公共前缀列表、分页信息等）
     * @throws RuntimeException 查询过程中发生异常时抛出
     */
    @Override
    public ListObjectsResult list(StorageProperties.StorageProvider providerConfig, ListObjectsRequest request) {
        try (StorageProvider storageProvider = runtimeStorageProviderFactory.create(providerConfig)) {
            // 本地存储使用文件系统直接操作
            if (isLocalLike(providerConfig.getType())) {
                return listLocal(providerConfig, storageProvider, request);
            }
            // 云存储调用SDK并过滤占位符
            ListObjectsResult result = storageProvider.list(request);
            return filterDirectoryPlaceholder(result);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to list storage objects", ex);
        }
    }

    /**
     * 获取对象元数据信息
     * <p>
     * 返回对象的详细信息，包括：大小、内容类型、最后修改时间、ETag等
     *
     * @param providerConfig 存储提供者配置
     * @param request        下载请求（包含对象键）
     * @return 对象元信息
     * @throws RuntimeException 获取过程中发生异常时抛出
     */
    @Override
    public ObjectInfo stat(StorageProperties.StorageProvider providerConfig, DownloadObjectRequest request) {
        try (StorageProvider storageProvider = runtimeStorageProviderFactory.create(providerConfig)) {
            return storageProvider.stat(request);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to stat storage object", ex);
        }
    }

    /**
     * 上传文件/对象到存储
     * <p>
     * 支持多种上传方式：字节数组、输入流、文件等（由 UploadContent 封装）
     *
     * @param providerConfig 存储提供者配置
     * @param request        上传请求（包含内容、对象键、元数据等）
     * @return 上传结果（包含ETag、版本ID等）
     * @throws RuntimeException 上传过程中发生异常时抛出
     */
    @Override
    public UploadResult upload(StorageProperties.StorageProvider providerConfig, UploadObjectRequest request) {
        try (StorageProvider storageProvider = runtimeStorageProviderFactory.create(providerConfig)) {
            return storageProvider.upload(request);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload storage object", ex);
        }
    }

    /**
     * 创建逻辑目录
     * <p>
     * 实现策略：
     * <ul>
     *   <li><b>本地存储</b>：使用 Files.createDirectories() 创建物理目录</li>
     *   <li><b>云存储</b>：在目标路径下创建 .peach-dir 占位符文件</li>
     * </ul>
     * 注意：云存储中的目录不是实际存在的实体，因此通过占位符文件来标识目录的存在
     *
     * @param providerConfig 存储提供者配置
     * @param path           目录路径（相对于存储桶根目录）
     * @throws RuntimeException 创建过程中发生异常时抛出
     */
    @Override
    public void createDirectory(StorageProperties.StorageProvider providerConfig, String path) {
        String relativePath = normalizeRelativePath(path);
        // 本地存储直接创建物理目录
        if (isLocalLike(providerConfig.getType())) {
            createLocalDirectory(providerConfig, relativePath);
            return;
        }
        // 云存储创建占位符文件
        String placeholderKey = StoragePathUtil.joinObjectKey(relativePath, DIRECTORY_PLACEHOLDER_FILE);
        UploadObjectRequest request = UploadObjectRequest.builder()
                .objectKey(placeholderKey)
                .content(UploadContent.of(new byte[0]))  // 空内容
                .contentType("application/octet-stream")
                .build();
        upload(providerConfig, request);
    }

    /**
     * 删除单个对象/文件
     *
     * @param providerConfig 存储提供者配置
     * @param request        删除请求（包含对象键）
     * @return 删除结果
     * @throws RuntimeException 删除过程中发生异常时抛出
     */
    @Override
    public DeleteResult deleteObject(StorageProperties.StorageProvider providerConfig, DeleteObjectRequest request) {
        try (StorageProvider storageProvider = runtimeStorageProviderFactory.create(providerConfig)) {
            return storageProvider.delete(request);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete storage object", ex);
        }
    }

    /**
     * 删除逻辑目录（递归删除）
     * <p>
     * 实现策略：
     * <ul>
     *   <li><b>本地存储</b>：递归遍历目录树，按逆序删除所有文件和子目录</li>
     *   <li><b>云存储</b>：分页递归列出所有子对象，然后批量删除，并清理占位符文件</li>
     * </ul>
     *
     * @param providerConfig 存储提供者配置
     * @param path           目录路径（相对于存储桶根目录）
     * @throws RuntimeException 删除过程中发生异常时抛出
     */
    @Override
    public void deleteDirectory(StorageProperties.StorageProvider providerConfig, String path) {
        String relativePath = normalizeRelativePath(path);
        // 本地存储直接删除物理目录
        if (isLocalLike(providerConfig.getType())) {
            deleteLocalDirectory(providerConfig, relativePath);
            return;
        }
        // 云存储：分页列出所有对象并批量删除
        try (StorageProvider storageProvider = runtimeStorageProviderFactory.create(providerConfig)) {
            List<String> objectKeys = new ArrayList<>();
            String continuationToken = null;
            // 分页递归列出目录下所有对象
            do {
                ListObjectsRequest request = ListObjectsRequest.builder()
                        .prefix(relativePath)
                        .recursive(true)
                        .maxKeys(1000)  // 每页最多1000个对象
                        .continuationToken(continuationToken)
                        .build();
                ListObjectsResult result = storageProvider.list(request);
                for (ObjectInfo item : result.getItems()) {
                    if (item == null || item.getObjectKey() == null) {
                        continue;
                    }
                    objectKeys.add(item.getObjectKey());
                }
                continuationToken = result.isTruncated() ? result.getNextContinuationToken() : null;
            } while (continuationToken != null);

            // 如果存在目录占位符文件，一并删除
            String placeholderKey = StoragePathUtil.joinObjectKey(relativePath, DIRECTORY_PLACEHOLDER_FILE);
            if (storageProvider.exists(placeholderKey)) {
                objectKeys.add(placeholderKey);
            }

            // 没有对象则直接返回
            if (objectKeys.isEmpty()) {
                return;
            }

            // 批量删除所有对象
            storageProvider.batchDelete(BatchDeleteObjectsRequest.builder()
                    .objectKeys(objectKeys)
                    .build());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete storage directory", ex);
        }
    }

    // ============================================================
    // 本地存储特有处理方法
    // ============================================================

    /**
     * 列出本地存储对象
     * <p>
     * 处理流程：
     * <ol>
     *   <li>解析根路径和基础路径</li>
     *   <li>如果基础路径不存在，返回空结果</li>
     *   <li>根据递归标志选择不同的列表策略</li>
     * </ol>
     *
     * @param providerConfig  存储提供者配置
     * @param storageProvider 存储提供者实例
     * @param request         列表请求
     * @return 列表查询结果
     */
    private ListObjectsResult listLocal(StorageProperties.StorageProvider providerConfig,
                                        StorageProvider storageProvider,
                                        ListObjectsRequest request) {
        Path rootPath = StoragePathUtil.parseLocalPath(providerConfig.getRootPath()).toAbsolutePath().normalize();
        Path basePath = resolveBasePath(providerConfig, request.getPrefix(), rootPath);

        // 路径不存在时返回空结果
        if (!Files.exists(basePath)) {
            return ListObjectsResult.builder()
                    .providerName(storageProvider.name())
                    .bucketName(storageProvider.bucketName(providerConfig))
                    .prefix(request.getPrefix())
                    .items(Collections.<ObjectInfo>emptyList())
                    .commonPrefixes(Collections.<String>emptyList())
                    .truncated(false)
                    .build();
        }

        // 根据递归标志分别处理
        return request.isRecursive()
                ? listLocalRecursive(providerConfig, storageProvider, request, rootPath, basePath)
                : listLocalCurrentLevel(providerConfig, storageProvider, request, rootPath, basePath);
    }

    /**
     * 递归列出本地存储对象（深度遍历）
     * <p>
     * 实现要点：
     * <ul>
     *   <li>使用 Files.walk() 递归遍历所有文件</li>
     *   <li>过滤掉 .peach-dir 占位符文件</li>
     *   <li>按对象键排序后，根据 continuationToken 进行分页截取</li>
     * </ul>
     *
     * @param providerConfig  存储提供者配置
     * @param storageProvider 存储提供者实例
     * @param request         列表请求
     * @param rootPath        根路径
     * @param basePath        基础路径（查询起始目录）
     * @return 分页列表结果
     */
    private ListObjectsResult listLocalRecursive(StorageProperties.StorageProvider providerConfig,
                                                 StorageProvider storageProvider,
                                                 ListObjectsRequest request,
                                                 Path rootPath,
                                                 Path basePath) {
        // 收集所有文件信息
        List<ObjectInfo> allObjects = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(basePath)) {
            stream.filter(Files::isRegularFile)  // 只处理文件，忽略目录
                    .map(path -> toLocalObjectInfo(providerConfig, storageProvider, rootPath, path))
                    .filter(objectInfo -> !isDirectoryPlaceholder(objectInfo.getObjectKey()))  // 过滤占位符
                    .sorted(Comparator.comparing(ObjectInfo::getObjectKey))  // 按对象键排序
                    .forEach(allObjects::add);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to list local directory", ex);
        }

        // 基于 continuationToken 进行分页截取
        List<ObjectInfo> pageItems = new ArrayList<>();
        int startIndex = findPageStartIndex(allObjects, request.getContinuationToken());
        int endIndex = Math.min(startIndex + request.getMaxKeys(), allObjects.size());
        pageItems.addAll(allObjects.subList(startIndex, endIndex));

        boolean truncated = endIndex < allObjects.size();
        String nextContinuationToken = pageItems.isEmpty() ? null : pageItems.get(pageItems.size() - 1).getObjectKey();

        return ListObjectsResult.builder()
                .providerName(storageProvider.name())
                .bucketName(storageProvider.bucketName(providerConfig))
                .prefix(request.getPrefix())
                .items(pageItems)
                .commonPrefixes(Collections.<String>emptyList())  // 递归模式下无公共前缀
                .truncated(truncated)
                .nextContinuationToken(nextContinuationToken)
                .build();
    }

    /**
     * 列出本地存储当前层级对象（非递归）
     * <p>
     * 只列出当前目录下的直接子项：
     * <ul>
     *   <li>子目录 → 加入 commonPrefixes（虚拟目录列表）</li>
     *   <li>文件（非占位符）→ 加入 items（对象列表）</li>
     * </ul>
     *
     * @param providerConfig  存储提供者配置
     * @param storageProvider 存储提供者实例
     * @param request         列表请求
     * @param rootPath        根路径
     * @param basePath        基础路径
     * @return 列表结果
     */
    private ListObjectsResult listLocalCurrentLevel(StorageProperties.StorageProvider providerConfig,
                                                    StorageProvider storageProvider,
                                                    ListObjectsRequest request,
                                                    Path rootPath,
                                                    Path basePath) {
        List<ObjectInfo> items = new ArrayList<>();
        Set<String> commonPrefixes = new LinkedHashSet<>();  // 使用LinkedHashSet保持顺序

        try (Stream<Path> stream = Files.list(basePath)) {
            for (Path child : stream.sorted().toList()) {
                if (Files.isDirectory(child)) {
                    // 子目录加入公共前缀列表
                    commonPrefixes.add(toBusinessPath(providerConfig, rootPath, child));
                    continue;
                }
                // 文件转为对象信息，过滤占位符
                ObjectInfo objectInfo = toLocalObjectInfo(providerConfig, storageProvider, rootPath, child);
                if (!isDirectoryPlaceholder(objectInfo.getObjectKey())) {
                    items.add(objectInfo);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to list local directory", ex);
        }

        return ListObjectsResult.builder()
                .providerName(storageProvider.name())
                .bucketName(storageProvider.bucketName(providerConfig))
                .prefix(request.getPrefix())
                .items(items)
                .commonPrefixes(new ArrayList<>(commonPrefixes))
                .truncated(false)  // 非递归模式下没有分页
                .build();
    }

    /**
     * 将本地文件路径转换为对象信息
     *
     * @param providerConfig  存储提供者配置
     * @param storageProvider 存储提供者实例
     * @param rootPath        根路径
     * @param path            文件路径
     * @return 对象信息（包含键、大小、内容类型、修改时间等）
     */
    private ObjectInfo toLocalObjectInfo(StorageProperties.StorageProvider providerConfig,
                                         StorageProvider storageProvider,
                                         Path rootPath,
                                         Path path) {
        try {
            return ObjectInfo.builder()
                    .providerName(storageProvider.name())
                    .bucketName(storageProvider.bucketName(providerConfig))
                    .objectKey(toBusinessPath(providerConfig, rootPath, path))
                    .size(Files.size(path))
                    .contentType(Files.probeContentType(path))
                    .lastModified(Files.getLastModifiedTime(path).toInstant())
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to build local object info", ex);
        }
    }

    /**
     * 将本地文件路径转换为业务对象键
     * <p>
     * 处理逻辑：
     * <ol>
     *   <li>计算相对于根路径的相对路径</li>
     *   <li>如果配置了前缀，需要从前缀中去除</li>
     * </ol>
     *
     * @param providerConfig 存储提供者配置
     * @param rootPath       根路径
     * @param path           实际文件路径
     * @return 业务对象键（相对于存储桶根目录）
     */
    private String toBusinessPath(StorageProperties.StorageProvider providerConfig, Path rootPath, Path path) {
        String actualObjectKey = StoragePathUtil.toObjectKey(rootPath, path);
        String normalizedPrefix = normalizeOptionalPath(providerConfig.getPrefix());
        if (normalizedPrefix == null || normalizedPrefix.isEmpty()) {
            return actualObjectKey;
        }
        // 如果实际键等于前缀，返回空字符串（表示根目录）
        if (actualObjectKey.equals(normalizedPrefix)) {
            return "";
        }
        // 如果实际键以"前缀/"开头，去除前缀部分
        if (actualObjectKey.startsWith(normalizedPrefix + "/")) {
            return actualObjectKey.substring(normalizedPrefix.length() + 1);
        }
        return actualObjectKey;
    }

    /**
     * 创建本地物理目录
     * <p>
     * 会自动创建不存在的父目录
     *
     * @param providerConfig 存储提供者配置
     * @param path           相对路径
     */
    private void createLocalDirectory(StorageProperties.StorageProvider providerConfig, String path) {
        Path rootPath = StoragePathUtil.parseLocalPath(providerConfig.getRootPath()).toAbsolutePath().normalize();
        String actualPath = applyConfiguredPrefix(providerConfig.getPrefix(), path);
        try {
            Files.createDirectories(StoragePathUtil.resolveLocalPath(rootPath, actualPath));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create local directory", ex);
        }
    }

    /**
     * 删除本地物理目录（递归删除）
     * <p>
     * 注意：使用逆序遍历，先删除文件再删除目录，避免删除非空目录失败
     *
     * @param providerConfig 存储提供者配置
     * @param path           相对路径
     */
    private void deleteLocalDirectory(StorageProperties.StorageProvider providerConfig, String path) {
        Path rootPath = StoragePathUtil.parseLocalPath(providerConfig.getRootPath()).toAbsolutePath().normalize();
        String actualPath = applyConfiguredPrefix(providerConfig.getPrefix(), path);
        Path targetPath = StoragePathUtil.resolveLocalPath(rootPath, actualPath);

        if (!Files.exists(targetPath)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(targetPath)) {
            // 按逆序排序（深度优先，先子后父）
            for (Path current : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(current);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete local directory", ex);
        }
    }

    // ============================================================
    // 辅助工具方法
    // ============================================================

    /**
     * 解析基础路径
     * <p>
     * 将配置的前缀和请求的前缀组合，解析出实际的文件系统路径
     *
     * @param providerConfig 存储提供者配置
     * @param requestPrefix  请求前缀
     * @param rootPath       根路径
     * @return 实际解析后的路径
     */
    private Path resolveBasePath(StorageProperties.StorageProvider providerConfig,
                                 String requestPrefix,
                                 Path rootPath) {
        String actualPath = applyConfiguredPrefix(providerConfig.getPrefix(), normalizeOptionalPath(requestPrefix));
        if (actualPath == null || actualPath.isEmpty()) {
            return rootPath;
        }
        return StoragePathUtil.resolveLocalPath(rootPath, actualPath);
    }

    /**
     * 应用配置的前缀到相对路径
     * <p>
     * 如果配置了前缀，将前缀和相对路径组合成完整路径
     *
     * @param configuredPrefix 配置的前缀
     * @param relativePath     相对路径
     * @return 组合后的路径
     */
    private String applyConfiguredPrefix(String configuredPrefix, String relativePath) {
        String normalizedPrefix = normalizeOptionalPath(configuredPrefix);
        String normalizedPath = normalizeOptionalPath(relativePath);
        if (normalizedPrefix == null || normalizedPrefix.isEmpty()) {
            return normalizedPath;
        }
        if (normalizedPath == null || normalizedPath.isEmpty()) {
            return normalizedPrefix;
        }
        return StoragePathUtil.joinObjectKey(normalizedPrefix, normalizedPath);
    }

    /**
     * 过滤列表结果中的目录占位符文件
     * <p>
     * 对用户隐藏 .peach-dir 占位符文件，保持浏览体验的一致性
     *
     * @param result 原始列表结果
     * @return 过滤后的列表结果
     */
    private ListObjectsResult filterDirectoryPlaceholder(ListObjectsResult result) {
        List<ObjectInfo> items = new ArrayList<>();
        for (ObjectInfo item : result.getItems()) {
            if (item == null || isDirectoryPlaceholder(item.getObjectKey())) {
                continue;
            }
            items.add(item);
        }
        return ListObjectsResult.builder()
                .providerName(result.getProviderName())
                .bucketName(result.getBucketName())
                .prefix(result.getPrefix())
                .items(items)
                .commonPrefixes(result.getCommonPrefixes())
                .truncated(result.isTruncated())
                .nextContinuationToken(result.getNextContinuationToken())
                .build();
    }

    /**
     * 判断对象键是否为目录占位符
     * <p>
     * 匹配两种格式：
     * <ul>
     *   <li>精确匹配：".peach-dir"</li>
     *   <li>路径匹配："xxx/xxx/.peach-dir"</li>
     * </ul>
     *
     * @param objectKey 对象键
     * @return true-是占位符；false-不是
     */
    private boolean isDirectoryPlaceholder(String objectKey) {
        return objectKey != null && objectKey.endsWith("/" + DIRECTORY_PLACEHOLDER_FILE)
                || DIRECTORY_PLACEHOLDER_FILE.equals(objectKey);
    }

    /**
     * 判断存储类型是否为本地类存储
     * <p>
     * 本地类存储包括 LOCAL 和 NAS，它们都基于文件系统实现
     *
     * @param storageType 存储类型枚举
     * @return true-是本地类；false-不是
     */
    private boolean isLocalLike(StorageType storageType) {
        return storageType == StorageType.LOCAL || storageType == StorageType.NAS;
    }

    /**
     * 规范化相对路径（严格模式）
     * <p>
     * 要求路径不能为空，否则抛出异常
     *
     * @param path 原始路径
     * @return 规范化后的路径（使用正斜杠，去除首尾斜杠）
     * @throws IllegalArgumentException 路径为空时抛出
     */
    private String normalizeRelativePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank");
        }
        return StoragePathUtil.normalizeObjectKey(path);
    }

    /**
     * 规范化可选路径（宽松模式）
     * <p>
     * 如果路径为空，返回 null 而不是抛出异常
     *
     * @param path 原始路径
     * @return 规范化后的路径，如果为空则返回 null
     */
    private String normalizeOptionalPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return StoragePathUtil.normalizeObjectKey(path);
    }

    private static int findPageStartIndex(List<ObjectInfo> allObjects, String continuationToken) {
        if (continuationToken == null || continuationToken.isBlank()) {
            return 0;
        }
        for (int i = 0; i < allObjects.size(); i++) {
            if (allObjects.get(i).getObjectKey().compareTo(continuationToken) > 0) {
                return i;
            }
        }
        return allObjects.size();
    }
}