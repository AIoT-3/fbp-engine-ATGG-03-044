package com.fbp.engine.network;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class TcpEchoServer {
    public static void main(String[] args) {
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Echo server started on port {}", port);
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(
                         new OutputStreamWriter(clientSocket.getOutputStream()))) {

                String message = reader.readLine();
                log.info("Server received: {}", message);

                writer.write(message);
                writer.newLine();
                writer.flush();
            }
        } catch (Exception e) {
            log.error("TCP echo server failed", e);
        }
    }
}
