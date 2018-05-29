package com.danielcs.socketserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class MessageBroker implements Runnable {

    private final Socket socket;
    private ArrayBlockingQueue<String> messages;

    public MessageBroker(Socket socket, ArrayBlockingQueue<String> messages) {
        this.socket = socket;
        this.messages = messages;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()
                )
        )) {

            System.out.println("Listening for incoming messages...");
            String msg;
            while ((msg = in.readLine()) != null) {
                messages.offer(msg); // ECHO
                System.out.println(msg); // SERVER LOG
            }
            System.out.println("Messagebroker stopped normally.");

        } catch (IOException e) {
            System.out.println("Messagebroker connection lost.");
        }
    }
}
