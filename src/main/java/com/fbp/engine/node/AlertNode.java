package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlertNode extends AbstractNode {
    public AlertNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected void onProcess(Message message) {
        String sensorId = message.get("sensorId");
        Double temperature = message.get("temperature");
        Double humidity = message.get("humidity");
        if(sensorId != null && temperature != null){
            log.warn("[경고] 센서 {} 온도 {}°C - 임계값 초과!", sensorId, temperature);
        }else if(sensorId != null && humidity != null) {
            log.warn("[경고] 센서 {} 습도 {}% - 임계값 초과!", sensorId, humidity);
        } else {
            log.warn("[경고] 알 수 없는 센서 데이터 출력");
        }
    }
}
