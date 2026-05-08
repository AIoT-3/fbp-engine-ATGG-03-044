package com.fbp.engine.plugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PluginClassLoader - 외부 JAR 클래스 로딩")
class PluginClassLoaderTest {
    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("외부 플러그인 JAR 안의 Provider 클래스를 로드한다")
    void loadsClassFromJar() throws Exception {
        Path jarPath = PluginJarTestSupport.createProviderJar(tempDirectory, "ExternalProvider", "ExternalCollector");

        try (PluginClassLoader classLoader = new PluginClassLoader(jarPath)) {
            Class<?> loadedClass = classLoader.loadClass("testplugin.ExternalProvider");

            assertEquals("testplugin.ExternalProvider", loadedClass.getName());
            assertSame(classLoader, loadedClass.getClassLoader());
        }
    }

    @Test
    @DisplayName("플러그인 클래스는 부모 ClassLoader의 엔진 SDK 인터페이스를 사용할 수 있다")
    void pluginClassCanUseEngineSdkFromParentClassLoader() throws Exception {
        Path jarPath = PluginJarTestSupport.createProviderJar(tempDirectory, "ExternalProvider", "ExternalCollector");

        try (PluginClassLoader classLoader = new PluginClassLoader(jarPath)) {
            Class<?> loadedClass = classLoader.loadClass("testplugin.ExternalProvider");
            Object instance = loadedClass.getConstructor().newInstance();

            assertInstanceOf(NodeProvider.class, instance);
        }
    }

    @Test
    @DisplayName("close 호출 시 ClassLoader 리소스를 정상 해제한다")
    void closeReleasesClassLoaderResource() throws Exception {
        Path jarPath = PluginJarTestSupport.createProviderJar(tempDirectory, "ExternalProvider", "ExternalCollector");
        PluginClassLoader classLoader = new PluginClassLoader(jarPath);

        assertDoesNotThrow(classLoader::close);
    }

    @Test
    @DisplayName("존재하지 않는 JAR 경로는 PluginException으로 거부한다")
    void missingJarFails() {
        Path missingJar = tempDirectory.resolve("missing.jar");

        assertThrows(PluginException.class, () -> new PluginClassLoader(missingJar));
    }
}
