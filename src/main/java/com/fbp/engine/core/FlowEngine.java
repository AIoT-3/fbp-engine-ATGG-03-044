package com.fbp.engine.core;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class FlowEngine {
    private Map<String, Flow> flows;
    private Map<String, State> flowStates;
    private State state;
    public FlowEngine(){
        this.flows = new HashMap<>();
        this.state = State.INITIALIZED;
        this.flowStates = new HashMap<>();
    }
    public void register(Flow flow){
        flows.put(flow.getId(), flow);
        flowStates.put(flow.getId(), State.STOPPED);
        log.info("[Engine] 플로우 '" + flow.getId() + "' 등록됨");
    }
    public void startFlow(String flowId) {
        Flow flow = flows.get(flowId);
        if (flow == null) {
            throw new IllegalArgumentException("플로우가 없습니다: " + flowId);
        }

        List<String> errors = flow.validate();
        if (!errors.isEmpty()) {
            throw new IllegalStateException("유효하지 않은 플로우: " + errors);
        }

        flow.initialize();
        flowStates.put(flowId, State.RUNNING);
        state = State.RUNNING;
        log.info("[Engine] 플로우 '" + flowId + "' 시작됨");
    }

    public void stopFlow(String flowId) {
        Flow flow = flows.get(flowId);
        if (flow == null) {
            throw new IllegalArgumentException("플로우가 없습니다: " + flowId);
        }

        flow.shutdown();
        flowStates.put(flowId, State.STOPPED);
        log.info("[Engine] 플로우 '" + flowId + "' 정지됨");
    }

    public void shutdown() {
        for (Flow flow : flows.values()) {
            flow.shutdown();
            flowStates.put(flow.getId(), State.STOPPED);
        }
        state = State.STOPPED;
    }
    public void listFlows(){
        for(String flowId : flows.keySet()){
            log.info(flowId + " - " + flowStates.get(flowId));
        }
    }

    public State getState() {
        return state;
    }

    public Map<String, Flow> getFlows() {
        return flows;
    }
    public Map<String, State> getFlowStates() {
        return flowStates;
    }

}
