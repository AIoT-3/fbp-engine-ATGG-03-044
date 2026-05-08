package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileWriterNodeTest {
    private FileWriterNode fileWriterNode;
    private String filePath;
    @BeforeEach
    void setUp() {
        filePath = "file-writer-test.log";
        fileWriterNode = new FileWriterNode("file-writer-1", filePath);

    }
    @Test
    @DisplayName("파일 생성")
    void FileCreateTest() {
        fileWriterNode.initialize();

        File file = new File(filePath);
        assertTrue(file.exists());

        fileWriterNode.shutdown();
        file.delete();
    }

    @Test
    @DisplayName("내용 기록")
    void WriteContentTest() throws Exception {
        fileWriterNode.initialize();

        fileWriterNode.process(new Message(Map.of("temperature", 21.5)));
        fileWriterNode.process(new Message(Map.of("temperature", 22.5)));
        fileWriterNode.process(new Message(Map.of("temperature", 23.5)));

        fileWriterNode.shutdown();

        List<String> lines = Files.readAllLines(Path.of(filePath));
        assertEquals(3, lines.size());

        File file = new File(filePath);
        file.delete();
    }

    @Test
    @DisplayName("shutdown 후 파일 닫힘")
    void ShutdownCloseTest() throws Exception {
        fileWriterNode.initialize();
        fileWriterNode.process(new Message(Map.of("temperature", 21.5)));
        fileWriterNode.shutdown();

        assertDoesNotThrow(() -> fileWriterNode.process(new Message(Map.of("temperature", 22.5))));

        List<String> lines = Files.readAllLines(Path.of(filePath));
        assertEquals(1, lines.size());

        File file = new File(filePath);
        file.delete();
    }
}
