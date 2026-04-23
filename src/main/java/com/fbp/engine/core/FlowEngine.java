package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class FlowEngine {
    private final Map<String, Flow> flows;
    private final Map<String, State> flowStates;
    private final Map<String, FlowRuntime> runtimes;
    private State state;
    public FlowEngine(){
        this.flows = new HashMap<>();
        this.state = State.INITIALIZED;
        this.flowStates = new HashMap<>();
        this.runtimes = new HashMap<>();
    }
    public void register(Flow flow){
        flows.put(flow.getId(), flow);
        flowStates.put(flow.getId(), State.STOPPED);
        log.info("[Engine] 플로우 '{}' 등록됨", flow.getId());
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

        if (flowStates.get(flowId) == State.RUNNING) {
            return;
        }

        flow.initialize();
        runtimes.put(flowId, startWorkers(flowId, flow));
        flowStates.put(flowId, State.RUNNING);
        state = State.RUNNING;
        log.info("[Engine] 플로우 '{}' 시작됨", flowId);
    }

    public void stopFlow(String flowId) {
        Flow flow = flows.get(flowId);
        if (flow == null) {
            throw new IllegalArgumentException("플로우가 없습니다: " + flowId);
        }

        stopWorkers(flowId);
        flow.shutdown();
        flowStates.put(flowId, State.STOPPED);
        if (flowStates.values().stream().noneMatch(flowState -> flowState == State.RUNNING)) {
            state = State.STOPPED;
        }
        log.info("[Engine] 플로우 '{}' 정지됨", flowId);
    }

    public void shutdown() {
        for (String flowId : flows.keySet()) {
            stopWorkers(flowId);
        }
        for (Flow flow : flows.values()) {
            flow.shutdown();
            flowStates.put(flow.getId(), State.STOPPED);
        }
        state = State.STOPPED;
    }
    public void listFlows(){
        for(String flowId : flows.keySet()){
            log.info("{} - {}", flowId, flowStates.get(flowId));
        }
    }

    public State getState() {
        return state;
    }

    public Map<String, Flow> getFlows() {
        return Collections.unmodifiableMap(flows);
    }

    public Map<String, State> getFlowStates() {
        return Collections.unmodifiableMap(flowStates);
    }

    private FlowRuntime startWorkers(String flowId, Flow flow) {
        AtomicBoolean running = new AtomicBoolean(true);
        List<Thread> workers = new ArrayList<>();

        for (Flow.ConnectionRoute route : flow.getConnectionRoutes()) {
            Thread worker = new Thread(() -> {
                while (running.get()) {
                    try {
                        Message message = route.getConnection().poll();
                        route.getTargetNode().getInputPort(route.getTargetPort()).receive(message);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (RuntimeException e) {
                        log.error("Worker failed while delivering {} -> {}:{}",
                                route.getConnection().getId(),
                                route.getTargetNode().getId(),
                                route.getTargetPort(),
                                e);
                    }
                }
            }, flowId + "-" + route.getConnection().getId());
            worker.start();
            workers.add(worker);
        }

        return new FlowRuntime(running, workers);
    }

    private void stopWorkers(String flowId) {
        FlowRuntime runtime = runtimes.remove(flowId);
        if (runtime == null) {
            return;
        }

        runtime.stop();

        boolean interrupted = false;
        for (Thread worker : runtime.workers()) {
            boolean joined = false;
            while (!joined) {
                try {
                    worker.join();
                    joined = true;
                } catch (InterruptedException e) {
                    interrupted = true;
                    Thread.currentThread().interrupt();
                    // Clear now so remaining workers can still be joined; restore at the end.
                    Thread.interrupted();
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static class FlowRuntime {
        private final AtomicBoolean running;
        private final List<Thread> workers;

        private FlowRuntime(AtomicBoolean running, List<Thread> workers) {
            this.running = running;
            this.workers = workers;
        }

        private void stop() {
            running.set(false);
            workers.forEach(Thread::interrupt);
        }

        private List<Thread> workers() {
            return workers;
        }
    }
}
