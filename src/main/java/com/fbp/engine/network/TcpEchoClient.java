package com.fbp.engine.network;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

@Slf4j
public class TcpEchoClient {
    public static void main(String[] args){
        String host = "localhost";
        int port = 12345;
        try (Socket socket = new Socket(host, port);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            String message = "Hello FBP";
            writer.write(message);
            writer.newLine();
            writer.flush();

            String response = reader.readLine();
            log.info("Client received: {}", response);

        } catch (Exception e) {
            log.error("TCP echo client failed", e);
        }
    }
}
