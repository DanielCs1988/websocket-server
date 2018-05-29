package com.danielcs.socketserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SocketServer {

    public final int PORT;
    public final int POOL_SIZE;

    private final Executor connectionPool;
    private final Map<Integer, ArrayBlockingQueue<String>> messages = Collections.synchronizedMap(new HashMap<>());

    public SocketServer(int port, int poolSize) {
        this.PORT = port;
        this.POOL_SIZE = poolSize;
        connectionPool = Executors.newFixedThreadPool(POOL_SIZE);
    }

    public void start() {

        try (ServerSocket server = new ServerSocket(PORT)) {

            int counter = 1;
            while (true) {
                Socket client = server.accept();
                messages.put(counter, new ArrayBlockingQueue<>(2));
                ConnectionHandler handler = new ConnectionHandler(client, messages.get(counter));
                connectionPool.execute(handler);
                messages.get(counter).offer("Welcome user number" + counter);
                System.out.println("Client connected: " + counter);

                if (counter == 2) {
                    for (Integer id : messages.keySet()) {
                        messages.get(id).offer("OH HI THERE IT DA BROADCASTED MESSAGE HERE!");
                        // TODO: make it non-blocking
                    }
                }

                if (counter > 3) {
                    for (Integer id : messages.keySet()) {
                        messages.get(id).offer("EOF");
                    }
                    break;
                }
                counter ++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server closed down.");
    }

    public static void main(String[] args) {
        SocketServer server = new SocketServer(5000, 10);
        server.start();
    }
}
