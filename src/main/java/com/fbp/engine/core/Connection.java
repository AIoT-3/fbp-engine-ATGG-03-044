package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import java.util.concurrent.LinkedBlockingQueue;

public class Connection {
    private String id;
    private LinkedBlockingQueue<Message> buffer;
    //private InputPort target;

    public Connection() {
        this(100);
    }

    public Connection(int capacity) {
        this.buffer = new LinkedBlockingQueue<>(capacity);
    }
    public Connection(String id){
        this.id = id;
        this.buffer = new LinkedBlockingQueue<>(100);
    }
    public Connection(String id, LinkedBlockingQueue<Message> buffer) {
        this.id = id;
        this.buffer = buffer;
    }
    public String getId(){
        return id;

    }
    public void deliver(Message message) throws InterruptedException {
        buffer.put(message);
    }
//    public void setTarget(InputPort target){
//        this.target = Objects.requireNonNull(target);
//    }
    public int getBufferSize(){
        return buffer.size();
    }
    public Message poll() throws InterruptedException {
        return buffer.take();
    }


}
