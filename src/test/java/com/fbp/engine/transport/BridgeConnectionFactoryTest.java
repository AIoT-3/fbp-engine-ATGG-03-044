package com.fbp.engine.transport;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.parser.ConnectionDefinition;
import com.fbp.engine.parser.NodeEndpoint;
import com.fbp.engine.parser.TransportDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("BridgeConnectionFactory - transport 설정에 따른 Connection 생성")
class BridgeConnectionFactoryTest {
    @Test
    @DisplayName("transport가 local이면 기존 BlockingQueue 기반 LocalConnection을 만든다")
    void localTransportCreatesLocalConnection() {
        BridgeConnectionFactory factory = new BridgeConnectionFactory();
        ConnectionDefinition connectionDefinition = connection("source", "out", "sink", "in");

        Connection connection = factory.create("flow-1", connectionDefinition, TransportDefinition.local());

        assertInstanceOf(LocalConnection.class, connection);
        assertEquals("source:out->sink:in", connection.getId());
    }

    @Test
    @DisplayName("MQTT 브릿지 토픽은 flowId와 from/to endpoint 이름으로 구성된다")
    void topicUsesFlowAndEndpointNames() {
        BridgeConnectionFactory factory = new BridgeConnectionFactory();
        NodeEndpoint from = new NodeEndpoint("sensor", "out");
        NodeEndpoint to = new NodeEndpoint("rule", "in");

        String topic = factory.topic("temperature-monitoring", from, to);

        assertEquals("fbp/temperature-monitoring/sensor.out->rule.in", topic);
    }

    @Test
    @DisplayName("MQTT 토픽에서 문제가 되는 와일드카드 문자는 안전한 문자로 치환된다")
    void topicReplacesMqttWildcardCharacters() {
        BridgeConnectionFactory factory = new BridgeConnectionFactory();
        NodeEndpoint from = new NodeEndpoint("sensor/1", "out+");
        NodeEndpoint to = new NodeEndpoint("rule#1", "in");

        String topic = factory.topic("flow/1", from, to);

        assertEquals("fbp/flow_1/sensor_1.out_->rule_1.in", topic);
    }

    private ConnectionDefinition connection(String sourceNode, String sourcePort,
                                            String targetNode, String targetPort) {
        NodeEndpoint from = new NodeEndpoint(sourceNode, sourcePort);
        NodeEndpoint to = new NodeEndpoint(targetNode, targetPort);
        return new ConnectionDefinition(from, to);
    }
}
