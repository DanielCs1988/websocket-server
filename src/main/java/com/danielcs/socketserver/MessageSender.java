package com.danielcs.socketserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class MessageSender implements Runnable {

    private final Socket socket;
    private ArrayBlockingQueue<String> messages;

    public MessageSender(Socket socket, ArrayBlockingQueue<String> messages) {
        this.socket = socket;
        this.messages = messages;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String msg;
            while (!(msg = messages.take()).startsWith("EOF")) {
                out.println(msg);
            }
            // TODO: this will cause trouble in a real application
            out.println("EOF");
            System.out.println(socket.getInetAddress() + " output module stopped normally.");

        } catch (IOException | InterruptedException e) {
            System.out.println("Output module connection lost.");
        }
    }
}
