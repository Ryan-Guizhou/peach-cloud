package com.peach.fileservice.common.util;

import com.peach.fileservice.common.FileDomainConstant;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * 文件Digest工具类。
 * <p>只处理输入流和摘要，不负责文件落盘、对象存储或日志输出。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public final class FileDigestUtils {

    private FileDigestUtils() {
    }

    /**
     * 计算 multipart 文件的 SHA-256 摘要。
     *
     * @param file multipart 文件
     * @return 小写十六进制 SHA-256 摘要
     */
    public static String sha256(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("upload file is empty");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return sha256(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("read upload file failed", ex);
        }
    }

    /**
     * 计算输入流的 SHA-256 摘要。方法不会关闭调用方传入的流。
     *
     * @param inputStream 输入流
     * @return 小写十六进制 SHA-256 摘要
     */
    public static String sha256(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("input stream is null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(FileDomainConstant.DIGEST_SHA256_ALGORITHM);
            byte[] buffer = new byte[FileDomainConstant.BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (Exception ex) {
            throw new IllegalStateException("calculate sha256 failed", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }
}
