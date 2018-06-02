package com.danielcs.socketserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import static com.danielcs.socketserver.Utils.encodeSocketStream;

class MessageSender implements Runnable {

    private final Socket socket;
    private UserSession user;

    public MessageSender(Socket socket, UserSession user) {
        this.socket = socket;
        this.user = user;
    }

    @Override
    public void run() {
        try (OutputStream out = socket.getOutputStream()) {

            String msg;
            ArrayBlockingQueue<String> messages = user.getMessages();
            while (!(msg = messages.take()).startsWith("EOF")) {
                out.write(encodeSocketStream(msg));
            }
            System.out.println("Output module connection broken from client side.");

        } catch (IOException | InterruptedException e) {
            System.out.println("Output module connection lost.");
        }
    }
}
