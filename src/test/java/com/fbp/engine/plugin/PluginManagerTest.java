package com.fbp.engine.plugin;

import com.fbp.engine.registry.NodeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PluginManager - ServiceLoader와 외부 JAR 플러그인 등록")
class PluginManagerTest {
    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("classpath의 BuiltInNodeProvider를 찾아 기본 노드를 Registry에 등록한다")
    void classPathPluginsRegisterBuiltInNodes() {
        NodeRegistry registry = new NodeRegistry();
        PluginManager manager = new PluginManager(registry, Path.of("missing-plugins"));

        int count = manager.loadClassPathPlugins();

        assertTrue(count > 0);
        assertTrue(registry.isRegistered("Generator"));
        assertTrue(registry.isRegistered("MqttSubscriber"));
    }

    @Test
    @DisplayName("plugins 디렉토리가 없어도 예외 없이 외부 플러그인 로드를 건너뛴다")
    void missingPluginDirectoryIsIgnored() {
        NodeRegistry registry = new NodeRegistry();
        PluginManager manager = new PluginManager(registry, Path.of("missing-plugins"));

        assertEquals(0, manager.loadExternalPlugins(Path.of("missing-plugins")));
        assertTrue(manager.getFailures().isEmpty());
    }

    @Test
    @DisplayName("plugins 디렉토리가 비어 있으면 등록 수 0, 실패 0으로 끝난다")
    void emptyPluginDirectoryLoadsZeroPlugins() {
        NodeRegistry registry = new NodeRegistry();
        PluginManager manager = new PluginManager(registry, tempDirectory);

        assertEquals(0, manager.loadExternalPlugins(tempDirectory));
        assertTrue(manager.getFailures().isEmpty());
    }

    @Test
    @DisplayName("외부 JAR의 NodeProvider가 제공하는 노드 타입을 Registry에 등록한다")
    void externalJarProviderRegistersNodeType() throws Exception {
        PluginJarTestSupport.createProviderJar(tempDirectory, "ExternalProvider", "ExternalCollector");
        NodeRegistry registry = new NodeRegistry();
        try (PluginManager manager = new PluginManager(registry, tempDirectory)) {
            int count = manager.loadExternalPlugins(tempDirectory);

            assertEquals(1, count);
            assertTrue(registry.isRegistered("ExternalCollector"));
            assertTrue(manager.getFailures().isEmpty());
        }
    }

    @Test
    @DisplayName("여러 외부 JAR을 로드하면 각 JAR의 노드 타입을 모두 등록한다")
    void multipleExternalJarsRegisterAllTypes() throws Exception {
        PluginJarTestSupport.createProviderJar(tempDirectory, "ExternalProviderA", "ExternalA");
        PluginJarTestSupport.createProviderJar(tempDirectory, "ExternalProviderB", "ExternalB");
        NodeRegistry registry = new NodeRegistry();
        try (PluginManager manager = new PluginManager(registry, tempDirectory)) {
            int count = manager.loadExternalPlugins(tempDirectory);

            assertEquals(2, count);
            assertTrue(registry.isRegistered("ExternalA"));
            assertTrue(registry.isRegistered("ExternalB"));
        }
    }

    @Test
    @DisplayName("이미 등록된 타입명과 충돌하면 실패 목록에 기록한다")
    void typeConflictIsRecordedAsFailure() throws Exception {
        PluginJarTestSupport.createProviderJar(tempDirectory, "ExternalProvider", "ExternalCollector");
        NodeRegistry registry = new NodeRegistry();
        registry.register("ExternalCollector", (id, config) -> new com.fbp.engine.node.CollectorNode(id));
        try (PluginManager manager = new PluginManager(registry, tempDirectory)) {
            int count = manager.loadExternalPlugins(tempDirectory);

            assertEquals(0, count);
            assertEquals(1, manager.getFailures().size());
            assertTrue(manager.getFailures().get(0).getMessage().contains("이미 등록된 노드 타입"));
        }
    }

    @Test
    @DisplayName("깨진 JAR은 실패로 기록하고 정상 JAR 로드는 계속 진행한다")
    void brokenJarIsRecordedAsFailureAndOtherJarsContinue() throws Exception {
        PluginJarTestSupport.createBrokenProviderJar(tempDirectory);
        PluginJarTestSupport.createProviderJar(tempDirectory, "ExternalProvider", "ExternalCollector");
        NodeRegistry registry = new NodeRegistry();
        try (PluginManager manager = new PluginManager(registry, tempDirectory)) {
            int count = manager.loadExternalPlugins(tempDirectory);

            assertEquals(1, count);
            assertTrue(registry.isRegistered("ExternalCollector"));
            assertEquals(1, manager.getFailures().size());
        }
    }
}
