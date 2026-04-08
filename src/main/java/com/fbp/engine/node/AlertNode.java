package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;

public class AlertNode extends AbstractNode {
    public AlertNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    protected void onProcess(com.fbp.engine.message.Message message) {
        String sensorId = message.get("sensorId");
        Double temperature = message.get("temperature");
        Double humidity = message.get("humidity");
        if(sensorId != null && temperature != null){
            System.out.println("[경고] 센서 " + sensorId + " 온도 " + temperature + "°C — 임계값 초과!");
        }else if(sensorId != null && humidity != null) {
            System.out.println("[경고] 센서 " + sensorId + " 습도 " + humidity + "% - 임계값 초과!");
        } else {
            System.out.println("[경고] 알 수 없는 센서 데이터 출력");
        }
    }
}
