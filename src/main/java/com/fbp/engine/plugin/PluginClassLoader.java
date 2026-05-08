package com.fbp.engine.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(Path jarPath) {
        super(new URL[] { toUrl(jarPath) }, PluginClassLoader.class.getClassLoader());
    }

    private static URL toUrl(Path jarPath) {
        if (jarPath == null || Files.notExists(jarPath) || !Files.isRegularFile(jarPath)) {
            throw new PluginException("플러그인 JAR가 없습니다: " + jarPath);
        }
        try {
            return jarPath.toUri().toURL();
        } catch (IOException e) {
            throw new PluginException("플러그인 JAR URL 변환 실패: " + jarPath, e);
        }
    }
}
