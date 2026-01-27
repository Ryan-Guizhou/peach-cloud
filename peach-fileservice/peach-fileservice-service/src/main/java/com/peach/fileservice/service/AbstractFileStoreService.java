package com.peach.fileservice.service;

import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/27 10:31
 * @Description 文件服务抽象类
 */
@Slf4j
public abstract class AbstractFileStoreService implements IFileStoreService {


    @Autowired(required = false)
    private IFileStoreSecurityStrategy fileStoreSecurityService;


//    protected abstract String doUploadFile(File file, String fileName);
//
//    protected abstract List<String> doUploadFile(File[] file, String targetPath);
//
//    protected abstract String doUpload(InputStream inputStream, String targetPath, String fileName);
//
//    protected abstract boolean isEnableClamav();
//
//    protected abstract String doUpload(String content, String targetPath, String fileName);

    protected String uploadFile(File file, String targetPath, String fileName){
        try (FileInputStream inputStream = new FileInputStream(file)){
            return uploadInputStream(inputStream, targetPath, fileName);
        }catch (Exception e){
            log.debug("uploadInputStream error:{}",e.getMessage());
            throw new RuntimeException(e);
        }
    }


    protected String uploadInputStream(InputStream inputStream, String targetPath, String fileName){
        return StringUtil.EMPTY;
    }

    /**
     * 检查文件是否被clamav 检测到
     * @param inputStream
     * @return
     */
    protected boolean checkForClamav(InputStream inputStream){
        return false;
    }


    private static final String PATH_SEPARATOR = "/";
    private static final Pattern DOUBLE_SLASH_PATTERN = Pattern.compile("/{2,}");
    private static final Pattern URL_PATTERN =  Pattern.compile("^https?://[^/]+/");
    private static final Pattern QUERY_PATTERN =  Pattern.compile("\\?.*");
    private static final Pattern PATH_SEPARATOR_PATTERN =
            Pattern.compile("\\\\");


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
        return targetPath + fileName;
    }


    /**
     *
     * @param path
     * @return
     */
    protected String removeUrlHost(String path) {
        if (path == null) {
            return null;
        }
        return DOUBLE_SLASH_PATTERN.matcher(path).replaceAll(PATH_SEPARATOR);
    }


}
