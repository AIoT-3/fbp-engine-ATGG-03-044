package com.fbp.engine.record;

import com.fbp.engine.core.Node;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;

    public class RecordingNode implements Node {
        private final List<Message> receivedMessages = new ArrayList<>();

        @Override
        public String getId() {
            return "recording-node";
        }

        @Override
        public void process(Message message) {
            receivedMessages.add(message);
        }

        public List<Message> getReceivedMessages() {
            return receivedMessages;
        }
        @Override
        public void initialize() {

        }

        @Override
        public void shutdown() {

        }
    }

