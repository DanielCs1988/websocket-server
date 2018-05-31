package com.danielcs.socketserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import static com.danielcs.socketserver.Utils.encodeSocketStream;

public class MessageSender implements Runnable {

    private final Socket socket;
    private ArrayBlockingQueue<String> messages;

    public MessageSender(Socket socket, ArrayBlockingQueue<String> messages) {
        this.socket = socket;
        this.messages = messages;
    }

    @Override
    public void run() {
        try (OutputStream out = socket.getOutputStream()) {

            String msg;
            while (!(msg = messages.take()).startsWith("EOF")) {
                out.write(encodeSocketStream(msg));
            }
            System.out.println(socket.getInetAddress() + " output module stopped normally.");

        } catch (IOException | InterruptedException e) {
            System.out.println("Output module connection lost.");
        }
    }
}
