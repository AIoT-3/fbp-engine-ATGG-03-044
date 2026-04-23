package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;
import com.fbp.engine.message.Message;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileWriterNode extends AbstractNode {
    private final String filePath;
    private BufferedWriter writer;
    private boolean closed;
    public FileWriterNode(String id, String filePath) {
        super(id);
        this.filePath = filePath;
        addInputPort("in");
    }
    @Override
    public void initialize() {
        try {
            writer = new BufferedWriter(new FileWriter(filePath, true));
            closed = false;
        } catch (IOException e) {
            throw new RuntimeException("파일 열기 실패: " + filePath, e);
        }
    }

    @Override
    protected synchronized void onProcess(Message message) {
        if (closed || writer == null) {
            return;
        }
        try{
            writer.write(message.toString());
            writer.newLine();
            writer.flush();
        }catch (IOException e){
            if (closed) {
                return;
            }
            throw new RuntimeException("파일 쓰기 실패: " + filePath, e);
        }
    }
    @Override
    public synchronized void shutdown(){
        closed = true;
        if(writer != null){
            try{
                writer.close();
            }catch (IOException e){
                throw new RuntimeException("파일 닫기 실패: " + filePath, e);
            } finally {
                writer = null;
            }
        }
    }

}
