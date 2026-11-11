package com.peach.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证文件上传内容的重复读取与资源释放。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 11:30
 */
class FileUploadContentLifecycleTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldCloseEveryPathStreamAfterRepeatedRead() throws IOException {
        Path file = Files.writeString(temporaryDirectory.resolve("content.txt"), "payload");
        PathContent content = new PathContent(file);
        InputStream first = content.read();
        InputStream second = content.read();

        content.close();
        content.close();

        assertThatThrownBy(first::read).isInstanceOf(IOException.class);
        assertThatThrownBy(second::read).isInstanceOf(IOException.class);
        assertThatThrownBy(content::read).isInstanceOf(IOException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void shouldCloseEveryFileStreamAfterRepeatedRead() throws IOException {
        Path file = Files.writeString(temporaryDirectory.resolve("file.txt"), "payload");
        FileContent content = new FileContent(file.toFile());
        InputStream first = content.read();
        InputStream second = content.read();

        content.close();

        assertThatThrownBy(first::read).isInstanceOf(IOException.class);
        assertThatThrownBy(second::read).isInstanceOf(IOException.class);
        assertThatThrownBy(content::read).isInstanceOf(IOException.class)
                .hasMessageContaining("closed");
    }
}
