package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeadLetterNode - 최종 처리 실패 메시지 보관")
class DeadLetterNodeTest {
    @Test
    @DisplayName("수신한 메시지를 순서대로 내부 저장소에 보관한다")
    void storesReceivedMessages() {
        DeadLetterNode node = new DeadLetterNode("dead-letter");
        Message first = new Message(Map.of("seq", 1));
        Message second = new Message(Map.of("seq", 2));

        node.process(first);
        node.process(second);

        List<Message> messages = node.getMessages();
        assertEquals(List.of(first, second), messages);
    }

    @Test
    @DisplayName("조회 결과는 수정 불가능한 복사본이라 내부 저장소를 외부에서 바꿀 수 없다")
    void returnsCopySoCallerCannotModifyInternalStorage() {
        DeadLetterNode node = new DeadLetterNode("dead-letter");
        Message message = new Message(Map.of("seq", 1));
        node.process(message);

        List<Message> messages = node.getMessages();

        assertThrows(UnsupportedOperationException.class, () -> messages.add(new Message(Map.of("seq", 2))));
        assertEquals(1, node.getMessages().size());
        assertSame(message, node.getMessages().get(0));
    }
}
