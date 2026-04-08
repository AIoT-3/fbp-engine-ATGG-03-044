package com.fbp.engine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Flow {
    private String id;
    private Map<String, AbstractNode> nodes;
    private List<Connection> connections;
    private final List<ConnectionInfo> connectionInfos;

    private enum VisitState{
        UNVISITED,
        VISITING,
        VISITED
    }

    private static class ConnectionInfo {
        private final String sourceNodeId;
        private final String sourcePort;
        private final String targetNodeId;
        private final String targetPort;
        private final Connection connection;

        private ConnectionInfo(String sourceNodeId, String sourcePort,
                               String targetNodeId, String targetPort,
                               Connection connection) {
            this.sourceNodeId = sourceNodeId;
            this.sourcePort = sourcePort;
            this.targetNodeId = targetNodeId;
            this.targetPort = targetPort;
            this.connection = connection;
        }
    }
    public Flow(String id) {
        this.id = id;
        this.nodes = new HashMap<>();
        this.connections = new ArrayList<>();
        this.connectionInfos = new ArrayList<>();
    }
    public Flow addNode(AbstractNode node){
        nodes.put(node.getId(),node);
        return this;
    }
    public Flow connect(String sourceNodeId, String sourcePort, String targetNodeId, String targetPort){
        AbstractNode sourceNode = nodes.get(sourceNodeId);
        AbstractNode targetNode = nodes.get(targetNodeId);
        if(sourceNode == null || sourceNode.getOutputPort(sourcePort) == null
                || targetNode == null || targetNode.getInputPort(targetPort)== null){
            throw new IllegalArgumentException("노드/포트 없음");
        }
        String connectionId = sourceNodeId + ":" + sourcePort + "->" +
                targetNodeId + ":" + targetPort;
        Connection connection = new Connection(connectionId);
        sourceNode.getOutputPort(sourcePort).connect(connection);
        connections.add(connection);
        connectionInfos.add(new ConnectionInfo(sourceNodeId, sourcePort, targetNodeId, targetPort, connection));
        return this;
    }
    public void initialize(){
        for(AbstractNode node : nodes.values()){
            node.initialize();
        }
    }
    public void shutdown(){
        for(AbstractNode node : nodes.values()){
            node.shutdown();
        }
    }
    public Map<String, AbstractNode> getNodes(){
        return nodes;
    }
    public String getId(){
        return id;
    }

    public List<Connection> getConnections(){
        return connections;
    }

    public List<String> validate(){
        List<String> error = new ArrayList<>();
        if(nodes.isEmpty()){
            error.add("노드가 없습니다");
        }
        Map<String, List<String>> graph = new HashMap<>();
        for(String nodeId : nodes.keySet()){
            graph.put(nodeId, new ArrayList<>());
        }
        for(ConnectionInfo info : connectionInfos){
            if(!nodes.containsKey(info.sourceNodeId)){
                error.add("출발 노드 없음");
                continue;
            }
            if(!nodes.containsKey(info.targetNodeId)){
                error.add("도착 노드 없음");
                continue;
            }
            graph.get(info.sourceNodeId).add(info.targetNodeId);
        }
        Map<String, VisitState> states = new HashMap<>();
        for(String nodeId : nodes.keySet()){
            states.put(nodeId,VisitState.UNVISITED);
        }
        for(String nodeId : nodes.keySet()){
            if(states.get(nodeId)==VisitState.UNVISITED){
                if(hasCycle(nodeId,graph,states)){
                    error.add("순환 참조가 있습니다.");
                    break;
                }
            }
        }
        return error;
    }
    private boolean hasCycle(String nodeId, Map<String, List<String>> graph, Map<String, VisitState> states) {
        states.put(nodeId, VisitState.VISITING);

        for (String nextNodeId : graph.get(nodeId)) {
            if (states.get(nextNodeId) == VisitState.VISITING) {
                return true;
            }
            if (states.get(nextNodeId) == VisitState.UNVISITED) {
                if (hasCycle(nextNodeId, graph, states)) {
                    return true;
                }
            }
        }

        states.put(nodeId, VisitState.VISITED);
        return false;
    }
}
