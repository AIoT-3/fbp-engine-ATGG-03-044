package com.fbp.engine.core;

import com.fbp.engine.node.AbstractNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Flow {
    private String id;
    private Map<String, AbstractNode> nodes;
    private List<Connection> connections;
    public Flow(String id) {
        this.id = id;
        this.nodes = new HashMap<>();
        this.connections = new ArrayList<>();
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
    public List<Connection> getConnections(){
        return connections;
    }
}
