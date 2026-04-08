package com.fbp.engine.node;

import com.fbp.engine.core.AbstractNode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileWriterNode extends AbstractNode {
    private final String filePath;
    private BufferedWriter writer;
    public FileWriterNode(String id, String filePath) {
        super(id);
        this.filePath = filePath;
        addInputPort("in");
    }
    @Override
    public void initialize() {
        try {
            writer = new BufferedWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            throw new RuntimeException("파일 열기 실패: " + filePath, e);
        }
    }

    @Override
    protected void onProcess(com.fbp.engine.message.Message message) {
        try{
            writer.write(message.toString());
            writer.newLine();
            writer.flush();
        }catch (IOException e){
            throw new RuntimeException("파일 쓰기 실패: " + filePath, e);
        }
    }
    @Override
    public void shutdown(){
        if(writer != null){
            try{
                writer.close();
            }catch (IOException e){
                throw new RuntimeException("파일 닫기 실패: " + filePath, e);
            }
        }
    }

}
