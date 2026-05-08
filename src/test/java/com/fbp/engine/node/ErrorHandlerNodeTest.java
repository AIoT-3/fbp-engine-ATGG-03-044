package com.fbp.engine.node;

import com.fbp.engine.core.Connection;
import com.fbp.engine.core.ErrorPort;
import com.fbp.engine.core.LocalConnection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorHandlerNode - 에러 메시지 retry/deadLetter 분기")
class ErrorHandlerNodeTest {
    @Test
    @DisplayName("retryCount가 maxRetries보다 작으면 retry 포트로 보내고 retryCount를 1 증가시킨다")
    void sendsToRetryWhenRetryCountIsBelowMaxRetries() throws InterruptedException {
        ErrorHandlerNode handler = new ErrorHandlerNode("handler", 2);
        Connection retry = new LocalConnection("retry");
        Connection deadLetter = new LocalConnection("dead-letter");
        handler.getOutputPort("retry").connect(retry);
        handler.getOutputPort("deadLetter").connect(deadLetter);

        handler.process(new Message(Map.of("retryCount", 0, "value", 10)));

        Message retryMessage = retry.poll();
        assertEquals(1, ((Number) retryMessage.get("retryCount")).intValue());
        assertEquals(10, ((Number) retryMessage.get("value")).intValue());
        assertEquals(0, deadLetter.getBufferSize());
    }

    @Test
    @DisplayName("retryCount가 없으면 0으로 보고 재시도 가능할 때 retry 포트로 보낸다")
    void sendsToRetryWhenRetryCountIsMissingAndRetriesRemain() throws InterruptedException {
        ErrorHandlerNode handler = new ErrorHandlerNode("handler", 1);
        Connection retry = new LocalConnection("retry");
        handler.getOutputPort("retry").connect(retry);

        handler.process(new Message(Map.of("value", 10)));

        Message retryMessage = retry.poll();
        assertEquals(1, ((Number) retryMessage.get("retryCount")).intValue());
    }

    @Test
    @DisplayName("재시도 횟수를 모두 사용했으면 deadLetter 포트로 보낸다")
    void sendsToDeadLetterWhenRetriesAreExhausted() throws InterruptedException {
        ErrorHandlerNode handler = new ErrorHandlerNode("handler", 2);
        Connection retry = new LocalConnection("retry");
        Connection deadLetter = new LocalConnection("dead-letter");
        handler.getOutputPort("retry").connect(retry);
        handler.getOutputPort("deadLetter").connect(deadLetter);

        handler.process(new Message(Map.of("retryCount", 2, "value", 10)));

        Message deadLetterMessage = deadLetter.poll();
        assertEquals(2, ((Number) deadLetterMessage.get("retryCount")).intValue());
        assertEquals(10, ((Number) deadLetterMessage.get("value")).intValue());
        assertEquals(0, retry.getBufferSize());
    }

    @Test
    @DisplayName("maxRetries가 음수이면 0으로 취급해 바로 deadLetter로 보낸다")
    void maxRetriesBelowZeroBehavesLikeZero() throws InterruptedException {
        ErrorHandlerNode handler = new ErrorHandlerNode("handler", -1);
        Connection deadLetter = new LocalConnection("dead-letter");
        handler.getOutputPort("deadLetter").connect(deadLetter);

        handler.process(new Message(Map.of("value", 10)));

        assertEquals(10, ((Number) deadLetter.poll().get("value")).intValue());
    }

    @Test
    @DisplayName("ErrorPort가 연결되어 있으면 에러 메시지를 Connection으로 전달할 수 있다")
    void errorPortCanDeliverErrorMessageWhenExplicitlyConnected() throws InterruptedException {
        ErrorPort errorPort = new ErrorPort("error");
        Connection connection = new LocalConnection("error-connection");
        Message errorMessage = new Message(Map.of(
                "sourceNodeId", "transform",
                "errorType", IllegalStateException.class.getName(),
                "errorMessage", "boom"
        ));
        errorPort.connect(connection);

        errorPort.send(errorMessage);

        Message received = connection.poll();
        assertEquals("transform", received.get("sourceNodeId"));
        assertEquals("boom", received.get("errorMessage"));
    }
}
