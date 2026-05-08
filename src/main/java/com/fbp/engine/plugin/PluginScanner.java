package com.fbp.engine.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class PluginScanner {
    public List<Path> scan(Path pluginDirectory) {
        if (pluginDirectory == null || Files.notExists(pluginDirectory)) {
            return List.of();
        }
        if (!Files.isDirectory(pluginDirectory)) {
            throw new PluginException("plugins 경로가 디렉토리가 아닙니다: " + pluginDirectory);
        }
        try (Stream<Path> paths = Files.list(pluginDirectory)) {
            return paths
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new PluginException("plugins 디렉토리 스캔 실패: " + pluginDirectory, e);
        }
    }
}
