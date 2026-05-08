package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

/**
 * 경고 메시지를 로그로 남기는 sink 노드다.
 * 의도적으로 출력 포트를 만들지 않는다.
 */
@Slf4j
public class AlertNode extends AbstractNode {
    public AlertNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected void onProcess(Message message) {
        String sensorId = firstText(message, "sensorId", "deviceInfo.deviceName", "deviceInfo.devEui");
        Double temperature = firstNumber(message, "temperature", "object.temperature");
        Double humidity = firstNumber(message, "humidity", "object.humidity");
        String measurementKey = message.get("measurement_key");
        Double value = numberValue(message.get("value"));
        if (sensorId == null) {
            sensorId = firstText(message, "device_name", "dev_eui", "device_eui");
        }
        if (temperature == null && "temperature".equals(measurementKey)) {
            temperature = value;
        }
        if (humidity == null && "humidity".equals(measurementKey)) {
            humidity = value;
        }
        if (sensorId != null && temperature != null) {
            log.warn("[경고] 센서 {} 온도 {}°C - 임계값 초과!", sensorId, temperature);
        } else if (sensorId != null && humidity != null) {
            log.warn("[경고] 센서 {} 습도 {}% - 임계값 초과!", sensorId, humidity);
        } else {
            log.warn("[경고] 알 수 없는 센서 데이터 출력");
        }
    }

    private String firstText(Message message, String firstPath, String secondPath, String thirdPath) {
        Object firstValue = message.get(firstPath);
        if (firstValue != null) {
            return String.valueOf(firstValue);
        }
        Object secondValue = message.get(secondPath);
        if (secondValue != null) {
            return String.valueOf(secondValue);
        }
        Object thirdValue = message.get(thirdPath);
        if (thirdValue != null) {
            return String.valueOf(thirdValue);
        }
        return null;
    }

    private Double firstNumber(Message message, String firstPath, String secondPath) {
        Double firstValue = numberValue(message.get(firstPath));
        if (firstValue != null) {
            return firstValue;
        }
        return numberValue(message.get(secondPath));
    }

    private Double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
