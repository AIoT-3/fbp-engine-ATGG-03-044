package com.fbp.engine.node;

import com.fbp.engine.core.ProtocolNode;
import com.fbp.engine.exception.ModbusException;
import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.ModbusTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class ModbusWriterNode extends ProtocolNode {
    private ModbusTcpClient client;

    public ModbusWriterNode(String id, Map<String, Object> config) {
        super(id, config);
        addInputPort("in");
        addOutputPort("result");
    }

    @Override
    protected void connect() throws Exception {
        String host = (String) getConfig("host");
        int port = getIntConfigOrDefault("port", 502);
        client = new ModbusTcpClient(host, port);
        client.connect();
    }

    @Override
    protected void disconnect() throws Exception {
        if (client != null) {
            client.disconnect();
        }
    }

    @Override
    protected void onProcess(Message message) {
        int slaveId = getIntConfigOrDefault("slaveId", 1);
        int registerAddress = getIntConfigOrDefault("registerAddress", 0);
        double scale = getDoubleConfigOrDefault("scale", 1.0);
        Integer fixedValue = getFixedValue();

        int registerValue;

        if (fixedValue != null) {
            registerValue = fixedValue;
        } else {
            String valueField = getStringConfigOrDefault("valueField", "value");
            Object valueObject = message.get(valueField);
            if (!(valueObject instanceof Number number)) {
                log.warn("MODBUS write failed: Missing numeric field: {}", valueField);
                return;
            }

            registerValue = (int) Math.round(number.doubleValue() * scale);
        }

        try {
            client.writeSingleRegister(slaveId, registerAddress, registerValue);
            send("result", new Message(Map.of(
                    "slaveId", slaveId,
                    "registerAddress", registerAddress,
                    "writtenValue", registerValue,
                    "timestamp", System.currentTimeMillis()
            )));
        } catch (ModbusException | IOException e) {
            log.warn("MODBUS write failed: {}", e.getMessage(), e);
        }
    }

    private int getIntConfigOrDefault(String key, int defaultValue) {
        Object value = getConfig(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private double getDoubleConfigOrDefault(String key, double defaultValue) {
        Object value = getConfig(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    private String getStringConfigOrDefault(String key, String defaultValue) {
        Object value = getConfig(key);
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return defaultValue;
    }

    private Integer getFixedValue() {
        Object value = getConfig("fixedValue");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
