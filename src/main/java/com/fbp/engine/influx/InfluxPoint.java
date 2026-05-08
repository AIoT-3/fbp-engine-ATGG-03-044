package com.fbp.engine.influx;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public class InfluxPoint {
    private final String measurement;
    private final Map<String, String> tags;
    private final Map<String, Object> fields;
    private final long timestampNanos;

    public InfluxPoint(String measurement, Map<String, String> tags,
                       Map<String, Object> fields, long timestampNanos) {
        this.measurement = requireText(measurement, "measurement는 필수입니다");
        if (tags == null) {
            this.tags = Map.of();
        } else {
            this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("InfluxDB field는 하나 이상 필요합니다");
        }
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        this.timestampNanos = timestampNanos;
    }

    public String toLineProtocol() {
        StringBuilder line = new StringBuilder();
        line.append(escapeMeasurement(measurement));
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            String key = requireText(tag.getKey(), "tag key는 비어 있을 수 없습니다");
            String value = requireText(tag.getValue(), "tag value는 비어 있을 수 없습니다");
            line.append(",");
            line.append(escapeTag(key));
            line.append("=");
            line.append(escapeTag(value));
        }

        line.append(" ");
        boolean firstField = true;
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (!firstField) {
                line.append(",");
            }
            String key = requireText(field.getKey(), "field key는 비어 있을 수 없습니다");
            line.append(escapeTag(key));
            line.append("=");
            line.append(formatFieldValue(field.getValue()));
            firstField = false;
        }

        if (timestampNanos > 0) {
            line.append(" ");
            line.append(timestampNanos);
        }
        return line.toString();
    }

    public String getMeasurement() {
        return measurement;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    private String formatFieldValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("field value는 null일 수 없습니다");
        }
        if (value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
            return value + "i";
        }
        if (value instanceof Float number) {
            validateFinite(number.doubleValue());
            return String.valueOf(number);
        }
        if (value instanceof Double number) {
            validateFinite(number);
            return String.valueOf(number);
        }
        if (value instanceof Boolean bool) {
            return bool.toString();
        }
        if (value instanceof String text) {
            return "\"" + escapeStringField(text) + "\"";
        }
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            validateFinite(doubleValue);
            return String.valueOf(doubleValue);
        }
        return "\"" + escapeStringField(value.toString()) + "\"";
    }

    private void validateFinite(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("InfluxDB 숫자 field는 NaN/Infinity일 수 없습니다");
        }
    }

    private String escapeMeasurement(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(" ", "\\ ")
                .replace(",", "\\,");
    }

    private String escapeTag(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(" ", "\\ ")
                .replace(",", "\\,")
                .replace("=", "\\=");
    }

    private String escapeStringField(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
