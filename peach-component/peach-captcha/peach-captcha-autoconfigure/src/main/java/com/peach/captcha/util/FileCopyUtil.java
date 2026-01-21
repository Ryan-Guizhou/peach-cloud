package com.peach.captcha.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import java.io.*;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/21 13:50
 */
@Slf4j
public final class FileCopyUtil {

    /** Default buffer size / 默认缓冲区大小 */
    private static final int BUFFER_SIZE = 4096;

    /** Utility class: prevent instantiation / 工具类禁止实例化 */
    private FileCopyUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Copy InputStream to OutputStream
     * 将输入流复制到输出流（自动关闭流）
     *
     * @param in  source input stream / 输入流
     * @param out target output stream / 输出流
     * @return number of bytes copied / 复制的字节数
     * @throws IOException IO exception
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        try {
            return StreamUtils.copy(in, out);
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    /**
     * Copy byte array to OutputStream
     * 将字节数组写入输出流
     *
     * @param in  source byte array / 字节数组
     * @param out target output stream / 输出流
     * @throws IOException IO exception
     */
    public static void copy(byte[] in, OutputStream out) throws IOException {
        try {
            out.write(in);
        } finally {
            closeQuietly(out);
        }
    }

    /**
     * Copy InputStream to byte array
     * 将输入流读取为字节数组
     *
     * @param in input stream / 输入流
     * @return byte array / 字节数组
     * @throws IOException IO exception
     */
    public static byte[] copyToByteArray(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
        copy(in, out);
        return out.toByteArray();
    }

    /**
     * Copy Reader to Writer
     * Reader → Writer 字符流复制
     *
     * @param in  reader / 输入字符流
     * @param out writer / 输出字符流
     * @return number of characters copied / 复制的字符数
     * @throws IOException IO exception
     */
    public static int copy(Reader in, Writer out) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];
        int totalChars = 0;

        try {
            int charsRead;
            while ((charsRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, charsRead);
                totalChars += charsRead;
            }
            out.flush();
            return totalChars;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    /**
     * Copy String to Writer
     * 将字符串写入 Writer
     *
     * @param in  source string / 输入字符串
     * @param out target writer / 输出字符流
     * @throws IOException IO exception
     */
    public static void copy(String in, Writer out) throws IOException {
        try {
            out.write(in);
        } finally {
            closeQuietly(out);
        }
    }

    /**
     * Copy Reader to String
     * Reader → String
     *
     * @param in reader / 输入字符流
     * @return string content / 字符串内容
     * @throws IOException IO exception
     */
    public static String copyToString(Reader in) throws IOException {
        if (in == null) {
            log.error("Input stream is null");
            return "";
        }
        StringWriter out = new StringWriter();
        copy(in, out);
        return out.toString();
    }

    /**
     * Delete file by file path
     * 根据文件路径删除文件
     *
     * @param fileUrl file path / 文件路径
     * @return true if deleted successfully / 删除成功返回 true
     */
    public static boolean deleteFile(String fileUrl) {
        try {
            File file = new File(fileUrl);
            if (file.isFile() && file.exists()) {
                return file.delete();
            }
            return false;
        } catch (Exception e) {
            log.error("Delete file error: {}", fileUrl, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Close Closeable quietly
     * 安静关闭资源（吞掉异常）
     *
     * @param closeable resource / 资源对象
     */
    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                log.error("Close resource error: {}", closeable, ignored);
            }
        }
    }
}
