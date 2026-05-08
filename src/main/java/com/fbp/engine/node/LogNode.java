package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.core.interfaces.InputPort;
import com.fbp.engine.core.interfaces.OutputPort;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogNode extends AbstractNode {
    public LogNode(String id){
        super(id);
        addInputPort("in");
        addOutputPort("out");
    }

    /**
     * 단계별 실습 코드에서 기본 입력 포트를 짧게 확인하기 위한 편의 메서드다.
     * 일반 연결 코드에서는 getInputPort("in")을 직접 사용해도 된다.
     */
    public InputPort getInputPort() {
        return getInputPort("in");
    }

    /**
     * 단계별 실습 코드에서 기본 출력 포트를 짧게 확인하기 위한 편의 메서드다.
     * 일반 연결 코드에서는 getOutputPort("out")을 직접 사용해도 된다.
     */
    public OutputPort getOutputPort() {
        return getOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {
        log.info("[{}] {}", getId(), message.getPayload());
        send("out", message);
    }
}
