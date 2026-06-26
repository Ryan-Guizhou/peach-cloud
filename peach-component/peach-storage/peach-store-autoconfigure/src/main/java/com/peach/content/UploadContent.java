package com.peach.content;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * 上传内容抽象。
 *
 * <p>该接口屏蔽上传数据来源，provider 只关心 {@link #read()} 返回的输入流和
 * {@link #length()} 返回的内容长度。调用方使用完成后应关闭该对象，以释放文件或流资源。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/10 14:54
 */
public interface UploadContent extends AutoCloseable {

    /**
     * 打开内容输入流。
     *
     * @return 输入流
     * @throws Exception 打开失败时抛出
     */
    InputStream read() throws Exception;

    /**
     * 内容长度，未知时返回 -1。
     *
     * @return 内容长度
     * @throws Exception 读取长度失败时抛出
     */
    default long length() throws Exception {
        return -1L;
    }

    /**
     * 关闭上传内容持有的资源。
     *
     * @throws Exception 关闭资源失败时抛出
     */
    @Override
    default void close() throws Exception {

    }

    /**
     * 使用本地 Path 创建上传内容。
     *
     * @param path 文件路径
     * @return 上传内容
     */
    static UploadContent of(Path path) {
        return new PathContent(path);
    }

    /**
     * 使用字符串创建上传内容。
     *
     * @param content 字符串内容
     * @param charset 字符集，为空时使用 UTF-8
     * @return 上传内容
     */
    static UploadContent of(String content, Charset charset) {
        return new StringContent(content, charset == null ? StandardCharsets.UTF_8 : charset);
    }

    /**
     * 使用字符串创建 UTF-8 上传内容。
     *
     * @param content 字符串内容
     * @return 上传内容
     */
    static UploadContent of(String content) {
        return of(content, StandardCharsets.UTF_8);
    }

    /**
     * 使用字节数组创建上传内容。
     *
     * @param bytes 字节数组
     * @return 上传内容
     */
    static UploadContent of(byte[] bytes) {
        return new ByteArrayContent(bytes);
    }

    /**
     * 使用文件创建上传内容。
     *
     * @param file 文件
     * @return 上传内容
     * @throws FileNotFoundException 文件不存在时抛出
     */
    static UploadContent of(File file) throws FileNotFoundException {
        return new FileContent(file);
    }

    /**
     * 使用输入流创建上传内容。
     *
     * @param inputStream 输入流
     * @param length 内容长度，未知时传 -1
     * @return 上传内容
     */
    static UploadContent of(InputStream inputStream, long length) {
        return new InputStreamContent(inputStream, length);
    }
}
