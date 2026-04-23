package com.fbp.engine.node;

import com.fbp.engine.core.ProtocolNode;
import com.fbp.engine.exception.ModbusException;
import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.ModbusTcpClient;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModbusReaderNode extends ProtocolNode {
    private ModbusTcpClient client;

    public ModbusReaderNode(String id, Map<String, Object> config) {
        super(id, config);
        addInputPort("trigger");
        addOutputPort("out");
        addOutputPort("error");
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
        int startAddress = getIntConfigOrDefault("startAddress", 0);
        int count = getIntConfigOrDefault("count", 1);

        try {
            int[] values = client.readHoldingRegisters(slaveId, startAddress, count);
            send("out", new Message(buildPayload(slaveId, startAddress, values)));
        } catch (ModbusException | IOException e) {
            send("error", new Message(Map.of(
                    "slaveId", slaveId,
                    "startAddress", startAddress,
                    "count", count,
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            )));
        }
    }

    private Map<String, Object> buildPayload(int slaveId, int startAddress, int[] values) {
        Object mappingObject = getConfig("registerMapping");
        if (mappingObject instanceof Map<?, ?> rawMapping) {
            return buildMappedPayload(slaveId, startAddress, values, rawMapping);
        }

        Map<String, Object> registers = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            registers.put(String.valueOf(startAddress + i), values[i]);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("slaveId", slaveId);
        payload.put("registers", registers);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }

    private Map<String, Object> buildMappedPayload(int slaveId, int startAddress, int[] values, Map<?, ?> rawMapping) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("slaveId", slaveId);

        for (int i = 0; i < values.length; i++) {
            String addressKey = String.valueOf(startAddress + i);
            Object mappingValue = rawMapping.get(addressKey);

            if (mappingValue instanceof Map<?, ?> definition) {
                String name = (String) definition.get("name");
                Object scaleObject = definition.get("scale");
                if (name != null) {
                    if (scaleObject instanceof Number scaleNumber) {
                        payload.put(name, values[i] * scaleNumber.doubleValue());
                    } else {
                        payload.put(name, values[i]);
                    }
                } else {
                    payload.put(addressKey, values[i]);
                }
            } else {
                payload.put(addressKey, values[i]);
            }
        }

        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }

    private int getIntConfigOrDefault(String key, int defaultValue) {
        Object value = getConfig(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}
