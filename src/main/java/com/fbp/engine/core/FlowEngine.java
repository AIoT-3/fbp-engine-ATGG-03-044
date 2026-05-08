package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.metrics.MessageMetrics;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class FlowEngine {
    private final Map<String, Flow> flows;
    private final Map<String, State> flowStates;
    private final Map<String, FlowRuntime> runtimes;
    private final MetricsCollector metricsCollector;
    private State state;
    public FlowEngine(){
        this(new MetricsCollector());
    }

    public FlowEngine(MetricsCollector metricsCollector){
        this.flows = new HashMap<>();
        this.state = State.INITIALIZED;
        this.flowStates = new HashMap<>();
        this.runtimes = new HashMap<>();
        this.metricsCollector = Objects.requireNonNull(metricsCollector, "metricsCollector must not be null");
    }
    public void register(Flow flow){
        flows.put(flow.getId(), flow);
        flowStates.put(flow.getId(), State.STOPPED);
        log.info("[Engine] 플로우 '{}' 등록됨", flow.getId());
    }

    public void unregister(String flowId) {
        Flow flow = flows.get(flowId);
        if (flow == null) {
            throw new IllegalArgumentException("플로우가 없습니다: " + flowId);
        }
        if (flowStates.get(flowId) == State.RUNNING) {
            stopFlow(flowId);
        }
        flows.remove(flowId);
        flowStates.remove(flowId);
        if (flowStates.values().stream().noneMatch(flowState -> flowState == State.RUNNING)) {
            state = flowStates.isEmpty() ? State.INITIALIZED : State.STOPPED;
        }
        log.info("[Engine] 플로우 '{}' 제거됨", flowId);
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

        attachMetrics(flowId, flow);
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
        detachMetrics(flow);
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
            detachMetrics(flow);
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

    public State getFlowState(String flowId) {
        State flowState = flowStates.get(flowId);
        if (flowState == null) {
            throw new IllegalArgumentException("플로우가 없습니다: " + flowId);
        }
        return flowState;
    }

    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    private FlowRuntime startWorkers(String flowId, Flow flow) {
        AtomicBoolean running = new AtomicBoolean(true);
        List<Thread> workers = new ArrayList<>();

        for (Flow.ConnectionRoute route : flow.getConnectionRoutes()) {
            Thread worker = new Thread(() -> {
                while (running.get()) {
                    String targetNodeId = route.getTargetNode().getId();
                    try {
                        Message message = route.getConnection().poll();
                        long byteCount = MessageMetrics.estimateBytes(message);
                        String wireId = route.getConnection().getId();
                        metricsCollector.setWireQueueSize(wireId, route.getConnection().getBufferSize());
                        metricsCollector.recordNodeInput(targetNodeId, route.getTargetPort(), byteCount);
                        metricsCollector.recordDomainMessage(flowId, targetNodeId, route.getTargetPort(), message);
                        metricsCollector.setQueueSize(targetNodeId, route.getConnection().getBufferSize());
                        long startNanos = metricsCollector.startTimer();
                        try {
                            route.getTargetNode().getInputPort(route.getTargetPort()).receive(message);
                            metricsCollector.recordProcessing(targetNodeId, startNanos, true);
                        } catch (RuntimeException e) {
                            metricsCollector.recordProcessing(targetNodeId, startNanos, false);
                            log.error("Worker failed while delivering {} -> {}:{}",
                                    route.getConnection().getId(),
                                    targetNodeId,
                                    route.getTargetPort(),
                                    e);
                        } finally {
                            metricsCollector.setQueueSize(targetNodeId, route.getConnection().getBufferSize());
                            metricsCollector.setWireQueueSize(wireId, route.getConnection().getBufferSize());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
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

    private void attachMetrics(String flowId, Flow flow) {
        for (AbstractNode node : flow.getNodes().values()) {
            node.attachMetrics(flowId, metricsCollector);
        }
    }

    private void detachMetrics(Flow flow) {
        for (AbstractNode node : flow.getNodes().values()) {
            node.detachMetrics();
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
