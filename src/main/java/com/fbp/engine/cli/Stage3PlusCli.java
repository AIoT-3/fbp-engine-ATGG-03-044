package com.fbp.engine.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fbp.engine.core.FlowEngine;
import com.fbp.engine.engine.FlowManager;
import com.fbp.engine.parser.JsonFlowParser;
import com.fbp.engine.plugin.BuiltInNodeProvider;
import com.fbp.engine.plugin.NodeDescriptor;
import com.fbp.engine.registry.NodeRegistry;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class Stage3PlusCli implements AutoCloseable {
    private final Stage3PlusCommandHandler commandHandler;
    private final AutoCloseable closeable;

    public Stage3PlusCli(Stage3PlusCommandHandler commandHandler) {
        this(commandHandler, () -> {
        });
    }

    public Stage3PlusCli(Stage3PlusCommandHandler commandHandler, AutoCloseable closeable) {
        this.commandHandler = commandHandler;
        this.closeable = closeable;
    }

    public static void main(String[] args) throws Exception {
        quietInfoLogs();
        try (Stage3PlusCli cli = runtimeCli()) {
            cli.run();
        }
    }

    private static void quietInfoLogs() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.WARN);
    }

    private static Stage3PlusCli runtimeCli() {
        NodeRegistry nodeRegistry = new NodeRegistry();
        BuiltInNodeProvider builtInNodeProvider = new BuiltInNodeProvider();
        for (NodeDescriptor descriptor : builtInNodeProvider.getNodeDescriptors()) {
            nodeRegistry.register(descriptor.getTypeName(), descriptor.getFactory());
        }

        FlowEngine flowEngine = new FlowEngine();
        FlowManager flowManager = new FlowManager(flowEngine, nodeRegistry);
        JsonFlowParser flowParser = new JsonFlowParser();
        Stage3InfluxRuntime influxRuntime = Stage3InfluxRuntime.fromEnvironment(flowManager);
        Stage3PlusCommandHandler commandHandler = new Stage3PlusCommandHandler(
                flowManager,
                flowParser,
                influxRuntime::statusText
        );
        if (influxRuntime.isEnabled()) {
            com.fbp.engine.influx.InfluxDbConfig config = influxRuntime.getConfig();
            System.out.println("InfluxDB exporter enabled: "
                    + config.getUrl()
                    + ", bucket="
                    + config.getBucket());
        }
        return new Stage3PlusCli(commandHandler, influxRuntime);
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("FBP Engine Stage 3+ CLI. Type help for commands.");

        while (true) {
            System.out.print("fbp> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine();
            if (commandHandler.isExitCommand(line)) {
                System.out.println("bye");
                break;
            }

            String output = commandHandler.handle(line);
            if (!output.isEmpty()) {
                System.out.print(output);
            }
        }
    }

    @Override
    public void close() throws Exception {
        closeable.close();
    }
}
