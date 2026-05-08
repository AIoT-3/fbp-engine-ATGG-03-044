package com.fbp.engine.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonFlowParser - JSON 플로우 정의 파싱과 검증")
class JsonFlowParserTest {
    private final JsonFlowParser parser = new JsonFlowParser();

    @Test
    @DisplayName("정상 JSON은 FlowDefinition의 기본 정보, 노드, 연결, config 타입을 보존한다")
    void parseValidFlow() {
        FlowDefinition definition = parser.parse(json("""
                {
                  "id": "flow-1",
                  "name": "Flow One",
                  "description": "test flow",
                  "nodes": [
                    { "id": "source", "type": "Generator", "config": { "enabled": true } },
                    { "id": "sink", "type": "Collector", "config": { "limit": 10 } }
                  ],
                  "connections": [
                    { "from": "source:out", "to": "sink:in" }
                  ]
                }
                """));

        assertEquals("flow-1", definition.getId());
        assertEquals("Flow One", definition.getName());
        assertEquals(2, definition.getNodes().size());
        assertEquals(1, definition.getConnections().size());
        assertEquals(TransportDefinition.LOCAL, definition.getTransport().getType());
        assertEquals(true, definition.getNode("source").orElseThrow().getConfig().get("enabled"));
        assertEquals(10, definition.getNode("sink").orElseThrow().getConfig().get("limit"));
    }

    @Test
    @DisplayName("metrics.domain 섹션은 도메인 메트릭 정의로 파싱된다")
    void parseDomainMetrics() {
        FlowDefinition definition = parser.parse(json("""
                {
                  "id": "iot-flow",
                  "metrics": {
                    "domain": [
                      {
                        "name": "temperature",
                        "source": { "node": "sensor", "port": "out" },
                        "field": "temperature",
                        "tags": { "unit": "celsius" },
                        "windows": ["1m", "1h", "1d"]
                      }
                    ]
                  },
                  "nodes": [
                    { "id": "sensor", "type": "MqttSubscriber" },
                    { "id": "sink", "type": "Collector" }
                  ],
                  "connections": [
                    { "from": "sensor:out", "to": "sink:in" }
                  ]
                }
                """));

        assertEquals(1, definition.getDomainMetrics().size());
        assertEquals("temperature", definition.getDomainMetrics().get(0).getName());
        assertEquals("sensor", definition.getDomainMetrics().get(0).getNodeId());
        assertEquals("out", definition.getDomainMetrics().get(0).getPortName());
        assertEquals("temperature", definition.getDomainMetrics().get(0).getField());
        assertEquals("celsius", definition.getDomainMetrics().get(0).getTags().get("unit"));
        assertEquals(3, definition.getDomainMetrics().get(0).getWindows().size());
    }

    @Test
    @DisplayName("Stage 3+ IoT 센서 플로우 JSON은 실제 설정 파일 기준으로 파싱된다")
    void parseStage3PlusIotSensorFlowConfig() throws Exception {
        try (InputStream inputStream = Files.newInputStream(Path.of("config/stage3-plus-iot-sensor-flow.json"))) {
            FlowDefinition definition = parser.parse(inputStream);

            assertEquals("stage3-plus-iot-sensor-flow", definition.getId());
            assertEquals(11, definition.getNodes().size());
            assertEquals(10, definition.getConnections().size());
            assertEquals(4, definition.getDomainMetrics().size());
            assertEquals("application/+/device/+/event/up",
                    definition.getNode("sensor").orElseThrow().getConfig().get("topic"));
            assertTrue(definition.getDomainMetrics().stream()
                    .anyMatch(metric -> "temperature".equals(metric.getName())));
            assertTrue(definition.getDomainMetrics().stream()
                    .anyMatch(metric -> "humidity".equals(metric.getName())));
            assertTrue(definition.getDomainMetrics().stream()
                    .anyMatch(metric -> "co2".equals(metric.getName())));
            assertTrue(definition.getDomainMetrics().stream()
                    .anyMatch(metric -> "door".equals(metric.getName())));
        }
    }

    @Test
    @DisplayName("transport.type=mqtt 설정은 MQTT 브릿지 전송 설정으로 파싱된다")
    void parseMqttTransport() {
        FlowDefinition definition = parser.parse(json("""
                {
                  "id": "flow-1",
                  "transport": {
                    "type": "mqtt",
                    "broker": "tcp://system-broker:1884",
                    "qos": 1
                  },
                  "nodes": [
                    { "id": "source", "type": "Generator" },
                    { "id": "sink", "type": "Collector" }
                  ],
                  "connections": [
                    { "from": "source:out", "to": "sink:in" }
                  ]
                }
                """));

        assertEquals(TransportDefinition.MQTT, definition.getTransport().getType());
        assertEquals("tcp://system-broker:1884", definition.getTransport().getBroker());
        assertEquals(1, definition.getTransport().getQos());
    }

    @Test
    @DisplayName("MQTT transport는 broker 값이 없으면 파싱에 실패한다")
    void mqttTransportRequiresBroker() {
        assertThrows(FlowParserException.class, () -> parser.parse(json("""
                {
                  "id": "flow-1",
                  "transport": { "type": "mqtt", "qos": 1 },
                  "nodes": [{ "id": "a", "type": "Generator" }]
                }
                """)));
    }

    @Test
    @DisplayName("플로우 id가 없으면 파싱에 실패한다")
    void missingIdFails() {
        assertThrows(FlowParserException.class, () -> parser.parse(json("""
                { "nodes": [{ "id": "a", "type": "Generator" }] }
                """)));
    }

    @Test
    @DisplayName("nodes 배열이 없으면 파싱에 실패한다")
    void missingNodesFails() {
        assertThrows(FlowParserException.class, () -> parser.parse(json("""
                { "id": "flow-1" }
                """)));
    }

    @Test
    @DisplayName("nodes 배열이 비어 있으면 파싱에 실패한다")
    void emptyNodesFails() {
        assertThrows(FlowParserException.class, () -> parser.parse(json("""
                { "id": "flow-1", "nodes": [] }
                """)));
    }

    @Test
    @DisplayName("JSON 문법이 깨져 있으면 파싱에 실패한다")
    void invalidJsonSyntaxFails() {
        assertThrows(FlowParserException.class, () -> parser.parse(json("""
                { "id": "flow-1", "nodes": [
                """)));
    }

    @Test
    @DisplayName("중복 노드 id가 있으면 파싱에 실패한다")
    void duplicateNodeIdFails() {
        assertThrows(FlowParserException.class, () -> parser.parse(json("""
                {
                  "id": "flow-1",
                  "nodes": [
                    { "id": "a", "type": "Generator" },
                    { "id": "a", "type": "Collector" }
                  ]
                }
                """)));
    }

    @Test
    @DisplayName("연결 문자열이 node:port 형식이 아니면 파싱에 실패한다")
    void invalidConnectionFormatFails() {
        assertThrows(FlowParserException.class, () -> parser.parse(json("""
                {
                  "id": "flow-1",
                  "nodes": [{ "id": "a", "type": "Generator" }],
                  "connections": [{ "from": "a", "to": "a:in" }]
                }
                """)));
    }

    @Test
    @DisplayName("연결이 존재하지 않는 노드를 참조하면 파싱에 실패한다")
    void unknownConnectionNodeFails() {
        assertThrows(FlowParserException.class, () -> parser.parse(json("""
                {
                  "id": "flow-1",
                  "nodes": [{ "id": "a", "type": "Generator" }],
                  "connections": [{ "from": "a:out", "to": "missing:in" }]
                }
                """)));
    }

    @Test
    @DisplayName("FlowDefinition의 노드/연결 목록은 외부에서 수정할 수 없다")
    void flowDefinitionCollectionsAreImmutable() {
        FlowDefinition definition = parser.parse(json("""
                {
                  "id": "flow-1",
                  "nodes": [
                    { "id": "source", "type": "Generator" },
                    { "id": "sink", "type": "Collector" }
                  ],
                  "connections": [
                    { "from": "source:out", "to": "sink:in" }
                  ]
                }
                """));

        assertThrows(UnsupportedOperationException.class,
                () -> definition.getNodes().add(new NodeDefinition("extra", "Collector", java.util.Map.of())));
        assertThrows(UnsupportedOperationException.class,
                () -> definition.getConnections().clear());
    }

    private ByteArrayInputStream json(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
