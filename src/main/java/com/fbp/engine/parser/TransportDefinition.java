package com.fbp.engine.parser;

public class TransportDefinition {
    public static final String LOCAL = "local";
    public static final String MQTT = "mqtt";

    private final String type;
    private final String broker;
    private final int qos;

    public TransportDefinition(String type, String broker, int qos) {
        this.type = ParserValidation.requireText(type, "transport type은 필수입니다").toLowerCase();
        this.broker = broker;
        this.qos = qos;

        if (!LOCAL.equals(this.type) && !MQTT.equals(this.type)) {
            throw new FlowParserException("지원하지 않는 transport type입니다: " + type);
        }
        if (MQTT.equals(this.type)) {
            ParserValidation.requireText(broker, "mqtt transport broker는 필수입니다");
            if (qos < 0 || qos > 2) {
                throw new FlowParserException("mqtt transport qos는 0, 1, 2 중 하나여야 합니다");
            }
        }
    }

    public static TransportDefinition local() {
        return new TransportDefinition(LOCAL, null, 0);
    }

    public boolean isMqtt() {
        return MQTT.equals(type);
    }

    public String getType() {
        return type;
    }

    public String getBroker() {
        return broker;
    }

    public int getQos() {
        return qos;
    }
}
