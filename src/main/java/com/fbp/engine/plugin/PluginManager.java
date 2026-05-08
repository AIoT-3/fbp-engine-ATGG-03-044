package com.fbp.engine.plugin;

import com.fbp.engine.registry.NodeRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class PluginManager implements AutoCloseable {
    private final NodeRegistry nodeRegistry;
    private final PluginScanner pluginScanner;
    private final Path pluginDirectory;
    private final List<PluginClassLoader> classLoaders = new ArrayList<>();
    private final List<PluginLoadFailure> failures = new ArrayList<>();

    public PluginManager(NodeRegistry nodeRegistry) {
        this(nodeRegistry, Path.of("plugins"), new PluginScanner());
    }

    public PluginManager(NodeRegistry nodeRegistry, Path pluginDirectory) {
        this(nodeRegistry, pluginDirectory, new PluginScanner());
    }

    public PluginManager(NodeRegistry nodeRegistry, Path pluginDirectory, PluginScanner pluginScanner) {
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry must not be null");
        this.pluginDirectory = pluginDirectory;
        this.pluginScanner = Objects.requireNonNull(pluginScanner, "pluginScanner must not be null");
    }

    public int loadPlugins() {
        failures.clear();
        int registeredCount = loadClassPathPlugins();
        registeredCount += loadExternalPlugins(pluginDirectory);
        return registeredCount;
    }

    public int loadClassPathPlugins() {
        return registerProviders(ServiceLoader.load(NodeProvider.class), "classpath", null);
    }

    public int loadExternalPlugins(Path directory) {
        int registeredCount = 0;
        for (Path jarPath : pluginScanner.scan(directory)) {
            try {
                PluginClassLoader classLoader = new PluginClassLoader(jarPath);
                classLoaders.add(classLoader);
                registeredCount += registerProviders(ServiceLoader.load(NodeProvider.class, classLoader), jarPath.toString(), classLoader);
            } catch (RuntimeException | ServiceConfigurationError e) {
                failures.add(new PluginLoadFailure(jarPath.toString(), e.getMessage()));
            }
        }
        return registeredCount;
    }

    public List<PluginLoadFailure> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    private int registerProviders(ServiceLoader<NodeProvider> providers, String source, ClassLoader expectedClassLoader) {
        int registeredCount = 0;
        try {
            for (NodeProvider provider : providers) {
                if (expectedClassLoader != null && provider.getClass().getClassLoader() != expectedClassLoader) {
                    continue;
                }
                for (NodeDescriptor descriptor : provider.getNodeDescriptors()) {
                    String typeName = descriptor.getTypeName();
                    nodeRegistry.register(typeName, descriptor.getFactory());
                    registeredCount++;
                }
            }
        } catch (RuntimeException | ServiceConfigurationError e) {
            failures.add(new PluginLoadFailure(source, e.getMessage()));
        }
        return registeredCount;
    }

    @Override
    public void close() {
        for (PluginClassLoader classLoader : classLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                failures.add(new PluginLoadFailure("close", e.getMessage()));
            }
        }
        classLoaders.clear();
    }
}
