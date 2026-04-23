package com.fbp.engine.node;

import com.fbp.engine.core.ProtocolNode;
import com.fbp.engine.message.Message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EchoProtocolNode extends ProtocolNode {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public EchoProtocolNode(String id, Map<String, Object> config){
        super(id, config);
        addInputPort("in");
        addOutputPort("out");
    }
    @Override
    protected void connect() throws Exception{
        String host = (String) getConfig("host");
        int port = (int) getConfig("port");
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }
    @Override
    protected void disconnect() throws Exception{
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
        if(socket != null && !socket.isClosed()){
            socket.close();
        }
    }
    @Override
    protected void onProcess(Message message) {
        try {
            Object payload = message.get("payload");
            String outbound = payload != null ? payload.toString() : message.getPayload().toString();

            writer.write(outbound);
            writer.newLine();
            writer.flush();

            String response = reader.readLine();
            if (response != null) {
                send("out", new Message(Map.of("response", response)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Echo transmission failed", e);
        }
    }
}
