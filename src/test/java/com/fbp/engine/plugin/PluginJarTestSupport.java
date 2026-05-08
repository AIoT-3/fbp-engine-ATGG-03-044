package com.fbp.engine.plugin;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class PluginJarTestSupport {
    private PluginJarTestSupport() {
    }

    static Path createProviderJar(Path directory, String providerClassName, String typeName) throws IOException {
        Path sourceDirectory = directory.resolve("src-" + providerClassName);
        Path classesDirectory = directory.resolve("classes-" + providerClassName);
        Files.createDirectories(sourceDirectory.resolve("testplugin"));
        Files.createDirectories(classesDirectory);

        Path sourceFile = sourceDirectory.resolve("testplugin").resolve(providerClassName + ".java");
        Files.writeString(sourceFile, providerSource(providerClassName, typeName), StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK compiler가 필요합니다");
        int result = compiler.run(
                null,
                null,
                null,
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                classesDirectory.toString(),
                sourceFile.toString()
        );
        assertEquals(0, result);

        Path jarPath = directory.resolve(providerClassName + ".jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addCompiledClasses(jar, classesDirectory);
            JarEntry serviceEntry = new JarEntry("META-INF/services/com.fbp.engine.plugin.NodeProvider");
            jar.putNextEntry(serviceEntry);
            jar.write(("testplugin." + providerClassName).getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarPath;
    }

    static Path createBrokenProviderJar(Path directory) throws IOException {
        Path jarPath = directory.resolve("broken-provider.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            JarEntry serviceEntry = new JarEntry("META-INF/services/com.fbp.engine.plugin.NodeProvider");
            jar.putNextEntry(serviceEntry);
            jar.write("missing.DoesNotExistProvider".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarPath;
    }

    private static String providerSource(String providerClassName, String typeName) {
        return """
                package testplugin;

                import com.fbp.engine.node.CollectorNode;
                import com.fbp.engine.plugin.NodeDescriptor;
                import com.fbp.engine.plugin.NodeProvider;
                import java.util.List;

                public class %s implements NodeProvider {
                    @Override
                    public List<NodeDescriptor> getNodeDescriptors() {
                        return List.of(new NodeDescriptor(
                                "%s",
                                "external test node",
                                CollectorNode.class,
                                (id, config) -> new CollectorNode(id)
                        ));
                    }
                }
                """.formatted(providerClassName, typeName);
    }

    private static void addCompiledClasses(JarOutputStream jar, Path classesDirectory) throws IOException {
        try (var paths = Files.walk(classesDirectory)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String entryName = classesDirectory.relativize(path).toString().replace('\\', '/');
                JarEntry entry = new JarEntry(entryName);
                jar.putNextEntry(entry);
                Files.copy(path, jar);
                jar.closeEntry();
            }
        }
    }
}
