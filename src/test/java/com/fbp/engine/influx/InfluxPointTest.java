package com.fbp.engine.influx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("InfluxPoint - measurement/tag/field를 line protocol로 변환")
class InfluxPointTest {
    @Test
    @DisplayName("measurement, tag, field, timestamp를 InfluxDB line protocol 문자열로 변환한다")
    void convertsMeasurementTagsFieldsAndTimestampToLineProtocol() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("flow_id", "flow 1");
        tags.put("node_id", "rule=1");

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("processed", 10L);
        fields.put("avg_time_ms", 2.5);
        fields.put("status", "ok");
        fields.put("running", true);

        InfluxPoint point = new InfluxPoint("node stats", tags, fields, 123L);

        assertEquals(
                "node\\ stats,flow_id=flow\\ 1,node_id=rule\\=1 "
                        + "processed=10i,avg_time_ms=2.5,status=\"ok\",running=true 123",
                point.toLineProtocol()
        );
    }

    @Test
    @DisplayName("field가 하나도 없는 point는 생성할 수 없다")
    void rejectsPointWithoutFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new InfluxPoint("node_stats", Map.of(), Map.of(), 1L));
    }

    @Test
    @DisplayName("NaN field 값은 line protocol 변환 시 거부한다")
    void rejectsNanFieldValue() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("value", Double.NaN);

        InfluxPoint point = new InfluxPoint("sensor_raw", Map.of(), fields, 1L);

        assertThrows(IllegalArgumentException.class, point::toLineProtocol);
    }
}
