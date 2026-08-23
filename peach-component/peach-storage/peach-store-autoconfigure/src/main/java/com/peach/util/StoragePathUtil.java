package com.peach.util;

import com.peach.exception.StorageException;
import com.peach.enums.StorageResultCode;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 存储路径工具类。
 *
 * <p>核心设计理念：
 * <ul>
 *   <li><b>统一分隔符：</b>对象存储（OSS/S3）统一使用 {@code /} 作为 Key 分隔符，不依赖操作系统</li>
 *   <li><b>安全防护：</b>严格过滤 {@code ..} 路径穿越符号，确保所有操作都在配置的根目录内</li>
 *   <li><b>跨平台兼容：</b>LOCAL/NAS 落盘时，将对象 Key 转换为当前操作系统的 {@link Path} 格式</li>
 * </ul>
 * </p>
 *
 * <p><b>使用示例：</b>
 * <pre>{@code
 * // 规范化对象Key
 * String key = StoragePathUtil.normalizeObjectKey("images//avatar/../photo.jpg");
 * // 输出: "images/photo.jpg"
 *
 * // 拼接对象Key
 * String fullKey = StoragePathUtil.joinObjectKey("user", "123", "file.txt");
 * // 输出: "user/123/file.txt"
 *
 * // 转换为本地路径（自动适配Windows/Linux）
 * Path localPath = StoragePathUtil.resolveLocalPath(Paths.get("/data"), "user/file.txt");
 * // Linux: /data/user/file.txt
 * // Windows: D:\\data\\user\\file.txt
 * }</pre>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public final class StoragePathUtil {

    private StoragePathUtil() {
        // 私有构造函数，防止实例化
    }

    /**
     * 规范化对象存储 Key。
     *
     * <p>处理规则：
     * <ol>
     *   <li>去除首尾多余的斜杠</li>
     *   <li>将 Windows 反斜杠 {@code \} 统一替换为正斜杠 {@code /}</li>
     *   <li>合并连续的多个斜杠为单个斜杠</li>
     *   <li>过滤当前目录符号 {@code .}</li>
     *   <li><b>拒绝越界符号 {@code ..}，抛出异常以防止路径穿越攻击</b></li>
     * </ol>
     * </p>
     *
     * @param objectKey 原始对象 Key，不可为 null 或空白
     * @return 规范化后的安全对象 Key
     * @throws StorageException 当 Key 为空白、仅为根路径或包含 {@code ..} 时抛出
     */
    public static String normalizeObjectKey(String objectKey) {
        // 1. 空值校验
        if (objectKey == null || objectKey.isBlank()) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Object key must not be blank");
        }

        // 2. 统一分隔符：\ -> /，并去除首尾空格
        String normalized = objectKey.trim().replace('\\', '/');

        // 3. 合并连续斜杠（如 "a//b" -> "a/b"）
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }

        // 4. 去掉首尾斜杠
        normalized = trimSlash(normalized);

        // 5. 防止 Key 仅为 "/" 或 "./" 等根路径
        if (normalized.isEmpty()) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Object key must not be root path");
        }

        // 6. 分段检查，过滤 "." 并拒绝 ".."
        List<String> parts = new ArrayList<>();
        for (String part : normalized.split("/")) {
            if (part.isEmpty() || ".".equals(part)) {
                continue; // 忽略当前目录符号
            }
            if ("..".equals(part)) {
                // 发现路径穿越，直接抛出异常（安全防御）
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Object key must not contain '..'");
            }
            parts.add(part);
        }

        return String.join("/", parts);
    }

    /**
     * 拼接多个对象 Key 片段为一个完整的规范化 Key。
     *
     * <p>自动处理片段间的分隔符，空片段会被自动忽略。</p>
     *
     * @param segments 零个或多个 Key 片段
     * @return 拼接并规范化后的完整 Key
     * @throws StorageException 当所有片段都为空时抛出
     */
    public static String joinObjectKey(String... segments) {
        if (segments == null || segments.length == 0) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Object key segment must not be empty");
        }

        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue; // 自动跳过空片段
            }
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(segment);
        }

        // 复用规范化逻辑，确保最终结果安全
        return normalizeObjectKey(builder.toString());
    }

    /**
     * 为对象 Key 拼接统一前缀（如业务模块名、环境标识等）。
     *
     * <p>典型场景：配置全局前缀后，业务代码只需传入相对路径即可。</p>
     *
     * @param prefix 配置前缀（如 "prod/images"），可为 null 或空
     * @param objectKey 业务对象 Key
     * @return 带前缀的规范化对象 Key
     */
    public static String applyPrefix(String prefix, String objectKey) {
        if (prefix == null || prefix.isBlank()) {
            return normalizeObjectKey(objectKey);
        }
        return joinObjectKey(prefix, objectKey);
    }

    /**
     * 将对象 Key 解析为本地文件系统路径（用于 LOCAL/NAS 存储模式）。
     *
     * <p><b>安全保障：</b>解析后会自动验证结果路径是否仍在根目录下，防止路径穿越攻击。</p>
     *
     * <p><b>示例：</b>
     * <pre>{@code
     * Path root = Paths.get("/data/storage");
     * Path result = resolveLocalPath(root, "user/avatar.jpg");
     * // Linux: /data/storage/user/avatar.jpg
     * // Windows: D:\\data\\storage\\user\\avatar.jpg
     * }</pre>
     *
     * @param rootPath 存储根目录（绝对路径），不可为 null
     * @param objectKey 对象 Key
     * @return 根目录下的本地文件路径
     * @throws StorageException 当对象 Key 解析后跳出根目录时抛出
     */
    public static Path resolveLocalPath(Path rootPath, String objectKey) {
        if (rootPath == null) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Root path must not be null");
        }

        // 1. 规范化根目录（转为绝对路径并去除 . 和 ..）
        Path normalizedRoot = rootPath.toAbsolutePath().normalize();

        // 2. 将对象 Key 中的 / 替换为当前系统文件分隔符，并与根目录拼接
        String localPath = normalizeObjectKey(objectKey).replace("/", java.io.File.separator);
        Path resolved = normalizedRoot.resolve(localPath).normalize();

        // 3. 安全检查：确保最终路径仍然在根目录下（防止 ../../ 等攻击）
        if (!resolved.startsWith(normalizedRoot)) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Object key is outside root path");
        }

        return resolved;
    }

    /**
     * 将本地文件路径反向转换为对象 Key。
     *
     * <p>适用于扫描目录后，获取文件相对于根目录的对象存储路径。</p>
     *
     * @param rootPath 存储根目录
     * @param filePath 文件绝对路径
     * @return 相对于根目录的对象 Key（使用 {@code /} 分隔）
     * @throws StorageException 当文件路径不在根目录下时抛出
     */
    public static String toObjectKey(Path rootPath, Path filePath) {
        Path normalizedRoot = rootPath.toAbsolutePath().normalize();
        Path normalizedFile = filePath.toAbsolutePath().normalize();

        if (!normalizedFile.startsWith(normalizedRoot)) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "File path is outside root path");
        }

        // 相对路径转换为对象 Key，统一使用 / 分隔
        String relativePath = normalizedRoot.relativize(normalizedFile).toString().replace('\\', '/');
        return normalizeObjectKey(relativePath);
    }

    /**
     * 将字符串解析为跨平台本地 Path 对象。
     *
     * <p><b>支持格式：</b>
     * <ul>
     *   <li>Windows 绝对路径：{@code C:\data\file.txt} 或 {@code \\server\share\file.txt}</li>
     *   <li>Linux/Unix 绝对路径：{@code /home/user/file.txt}</li>
     *   <li>相对路径：{@code ./data/file.txt} 或 {@code ../file.txt}</li>
     *   <li>URI 格式：{@code file:///tmp/a.txt} 自动转换为本地路径</li>
     * </ul>
     * </p>
     *
     * @param path 路径字符串，不可为 null 或空白
     * @return 本地文件系统 Path 对象
     * @throws StorageException 当路径格式非法或 URI 解析失败时抛出
     */
    public static Path parseLocalPath(String path) {
        if (path == null || path.isBlank()) {
            throw new StorageException(StorageResultCode.BAD_REQUEST, "Local path must not be blank");
        }

        String trimmed = path.trim();

        // 处理 file:// 协议的 URI
        if (trimmed.startsWith("file:")) {
            try {
                return Paths.get(new URI(trimmed));
            } catch (URISyntaxException | IllegalArgumentException ex) {
                throw new StorageException(StorageResultCode.BAD_REQUEST, "Invalid file URI: " + path, ex);
            }
        }

        return Paths.get(trimmed);
    }

    /**
     * 判断路径字符串是否为 Windows 绝对路径。
     *
     * <p><b>匹配规则：</b>
     * <ul>
     *   <li>盘符形式：{@code C:\data}、{@code D:/data}</li>
     *   <li>UNC 网络路径：{@code \\server\share\file}</li>
     * </ul>
     * </p>
     *
     * @param path 路径字符串
     * @return true 表示是 Windows 绝对路径，false 表示不是
     */
    public static boolean isWindowsAbsolutePath(String path) {
        if (path == null || path.length() < 2) {
            return false;
        }
        // 正则：盘符 + : + /或\ 开头，或者以 \\ 开头（UNC路径）
        return path.matches("^[a-zA-Z]:[\\\\/].*") || path.startsWith("\\\\");
    }

    /**
     * 判断路径字符串是否为 Linux/Unix 绝对路径。
     *
     * @param path 路径字符串
     * @return true 表示以 {@code /} 开头（即 Unix 绝对路径）
     */
    public static boolean isUnixAbsolutePath(String path) {
        return path != null && path.startsWith("/");
    }

    /**
     * 去除字符串首尾的斜杠（仅处理 {@code /}）。
     *
     * @param value 待处理的字符串
     * @return 去除首尾斜杠后的字符串
     */
    private static String trimSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}