package com.danielcs.socketserver;

import java.io.*;
import java.net.Socket;
import java.util.Map;

import static com.danielcs.socketserver.SocketTransactionUtils.decodeSocketStream;

class MessageBroker implements Runnable {

    // TODO: Make buffer size dynamic
    private static final int BUFFER_SIZE = 4096;

    private final MessageFormatter msgFormatter;
    private final Socket socket;
    private final BasicContext context;
    private final Map<String, Controller> controllers;
    private Caller connectHandler;
    private Caller disconnectHandler;

    MessageBroker(
            Socket socket,
            BasicContext ctx,
            Map<String, Controller> controllers,
            Caller connectHandler,
            Caller disconnectHandler,
            MessageFormatter msgFormatter
    ) {
        this.socket = socket;
        this.context = ctx;
        this.controllers = controllers;
        this.connectHandler = connectHandler;
        this.disconnectHandler = disconnectHandler;
        this.msgFormatter = msgFormatter;
    }

    private void processMessage(String msg) {
        try {
            msgFormatter.processMessage(msg);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        Controller controller = controllers.get(msgFormatter.getRoute());
        if (controller != null) {
            context.setCurrentRoute(msgFormatter.getRoute());
            controller.handle(context, msgFormatter.getRawPayload());
        }
    }

    private void onConnect() {
        if (connectHandler != null) {
            connectHandler.call(context);
            connectHandler = null;
        }
    }

    private void onDisconnect() {
        context.getUser().sendMessage("EOF");
        if (disconnectHandler != null) {
            disconnectHandler.call(context);
        }
        context.removeUser();
    }

    @Override
    public void run() {
        try (
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                OutputStream out = socket.getOutputStream()
        ) {

            boolean isConnectionValid = SocketTransactionUtils.handleHandshake(inputStream, out, context);
            if (!isConnectionValid) {
                System.out.println("Invalid handshake attempt was received. Thread broken.");
                return;
            }
            
            byte[] stream = new byte[BUFFER_SIZE];
            int inputLength;
            String msg;

            onConnect();
            System.out.println("Listening for incoming messages...");

            while (context.connected()) {
                inputLength = inputStream.read(stream);
                if (inputLength != -1) {
                    msg = decodeSocketStream(stream, inputLength);
                    if (msg == null) {
                        break;
                    }
                    processMessage(msg);
                    stream = new byte[BUFFER_SIZE];
                }
            }
            System.out.println("Messagebroker stopped normally.");

        } catch (IOException e) {
            System.out.println("Messagebroker connection lost.");
        } finally {
            onDisconnect();
        }
    }
}
