package com.peach.fileservice.service;

import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.util.regex.Pattern;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 10:31
 * @Description 文件服务抽象类
 */
@Slf4j
public abstract class AbstractFileStoreService implements IFileStoreService {

    /**
     * 文件存储服务默认过期时间 2年 / The default expiration time for file storage service is 2 years
     */
    protected static final long EXPIRATION = 3600L * 1000 * 24 * 365 * 2;

    /**
     * 路径分隔符 / Path separator
     */
    protected static final String PATH_SEPARATOR = "/";

    /**
     * 双斜杠正则 / Double slash regular
     */
    protected static final Pattern DOUBLE_SLASH_PATTERN = Pattern.compile("/{2,}");

    /**
     * URL正则 / URL regular
     */
    protected static final Pattern URL_PATTERN =  Pattern.compile("^https?://[^/]+/");

    /**
     * 查询参数正则 / Query parameter regular
     */
    protected static final Pattern QUERY_PATTERN =  Pattern.compile("\\?.*");

    /**
     * 路径分隔符正则 / Path separator regular
     */
    protected static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile("\\\\");


    @Autowired(required = false)
    private IFileStoreSecurityStrategy fileStoreSecurityService;


    /**
     * 获取子类的存储路径的前缀
     * @return
     */
    protected abstract String prefix();

    /**
     * 获取子类存储的代理地址
     * @return
     */
    protected abstract String proxyHost();


    /**
     * 上传文件流 / Upload file stream
     * @param inputStream 文件流 / File stream
     * @param targetPath 目标路径 / Target path
     * @param fileName 文件名 / File name
     * @return 文件存储路径 / File storage path
     */
    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName){
        return StringUtil.EMPTY;
    }

    /**
     * 获取原始文件URL / Get the original file URL
     * @param key
     * @param isUrl
     * @return
     */
    protected String getOrgUrlByKey(String key, boolean isUrl) {
        return StringUtil.EMPTY;
    }


    /**
     * 上传文件 / Upload file
     * @param file 文件 /  File
     * @param targetPath 目标路径 / Target path
     * @param fileName 文件名 / File name
     * @return 文件存储路径 / File storage path
     */
    protected String uploadFile(File file, String targetPath, String fileName){
        try (FileInputStream inputStream = new FileInputStream(file)){
            return uploadInputStream(inputStream, targetPath, fileName);
        }catch (Exception e){
            log.debug("uploadInputStream error:{}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查文件是否被clamav 检测到
     * @param inputStream
     * @return
     */
    protected boolean checkForClamav(InputStream inputStream){
        return false;
    }


    /**
     * 标准化路径（统一使用 / 分隔符）
     * @param path
     * @return
     */
    protected String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return StringUtil.EMPTY;
        }
        // 去掉参数
       path = QUERY_PATTERN.matcher( path).replaceAll( "");

        // 去掉协议和域名（如果是URL）
        path = URL_PATTERN.matcher(path).replaceAll("");

        // 统一分隔符
        path = PATH_SEPARATOR_PATTERN.matcher(path).replaceAll(PATH_SEPARATOR);

        // 去掉多余的 /
        path = DOUBLE_SLASH_PATTERN.matcher(path).replaceAll(PATH_SEPARATOR);

        // 去掉开头的 /
        if (path.startsWith(PATH_SEPARATOR)) {
            path = path.substring(1);
        }

        return path;
    }

    /**
     * 构建完整的对象Key
     * @param targetPath
     * @param fileName
     * @return
     */
    protected String buildPathKey(String targetPath, String fileName){
        targetPath = targetPath.endsWith(PATH_SEPARATOR) ? targetPath : PATH_SEPARATOR + targetPath;
        String finalPath = prefix() + targetPath + fileName;
        return normalizePath(finalPath);
    }


    /**
     * 移除URL中的域名 / Remove the domain name from the URL
     * @param path 要处理的URL / URL to be processed
     * @return 处理后的URL / Processed URL
     */
    protected String removeUrlHost(String path) {
        if (path == null) {
            return null;
        }
        path = URL_PATTERN.matcher(path).replaceAll(PATH_SEPARATOR);
        return QUERY_PATTERN.matcher(path).replaceAll(path);
    }

    /**
     * 替换URL中的域名 / Replace the host name in the URL
     * @param url 要替换的URL / URL to be replaced
     * @return 替换后的URL / Replaced URL
     */
    protected String replaceUrlHost(String url, boolean isUrl) {
        if (url == null) {
            return null;
        }
        url = URL_PATTERN.matcher(url).replaceAll(PATH_SEPARATOR);
        String replacePath = isUrl ? "" : proxyHost();
        return QUERY_PATTERN.matcher(url).replaceAll(replacePath);
    }


    /**
     * 删除时检查路径是否合法 / Check if the path is valid when deleting
     * @param key 要删除的路径或者是文件路径 / The path or file path to be deleted
     * @return true 表示路径合法，false 表示路径非法 / True means the path is legal, false means the path is illegal
     */
    protected boolean isHasIllegalChar(String key) {
        try {
            if (StringUtil.isBlank(key)) {
                throw new IllegalArgumentException("Path cannot be blank");
            }

            // 清理路径前后的空格，并统一为正斜杠 / Clean up the path and standardize it to a forward slash
            key = normalizePath(key);

            // 不允许删除根目录或上级目录 / Do not allow deletion of root directory or parent directory
            if (key.equals("/") || key.equals("..") || key.equals(".") || key.equals("")) {
                throw new IllegalArgumentException("Path cannot be /, .., or .");
            }

            // 不允许路径中包含 .. 或 ./ 等不规范路径 / Do not allow paths containing .. or ./ etc.
            if (key.contains("/../") || key.contains("/./") || key.contains("//")) {
                throw new IllegalArgumentException("Path contains invalid sequences like ../ or //");
            }

            // 可选：不允许路径结尾为 .. 或 . / Optional: Do not allow paths ending with .. or .
            if (key.endsWith("/..") || key.endsWith("/.")) {
                throw new IllegalArgumentException("Path should not end with .. or .");
            }
        }catch (Exception e){
            log.error("check file path is illegal,path:{}",key,e);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

}
