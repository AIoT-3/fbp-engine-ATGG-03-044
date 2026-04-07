package com.fbp.engine.Node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.DefaultInputPort;
import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.GeneratorNode;
import com.fbp.engine.record.RecordingNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GeneratorNodeTest {
    private GeneratorNode generatorNode;

    @BeforeEach
    void setUp() {
        generatorNode = new GeneratorNode("generator-1");
    }

    @Test
    @DisplayName("generate 메시지 생성")
    void GenerateMessageTest() {
        Message message = generatorNode.createMessage("key", "value");

        assertNotNull(message);
    }

    @Test
    @DisplayName("메시지 내용 확인")
    void MessageContentTest() {
        Message message = generatorNode.createMessage("key", "value");

        assertEquals("value", message.get("key"));
    }

    @Test
    @DisplayName("OutputPort 조회")
    void OutputPortTest() {
        assertNotNull(generatorNode.getOutputPort());
    }

    @Test
    @DisplayName("다수 generate 호출")
    void MultiGenerateTest() {
        Message first = generatorNode.createMessage("seq", 1);
        Message second = generatorNode.createMessage("seq", 2);
        Message third = generatorNode.createMessage("seq", 3);

        assertEquals(Integer.valueOf(1), first.get("seq"));
        assertEquals(Integer.valueOf(2), second.get("seq"));
        assertEquals(Integer.valueOf(3), third.get("seq"));
    }
}

