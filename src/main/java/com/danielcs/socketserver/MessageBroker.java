package com.danielcs.socketserver;

import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static com.danielcs.socketserver.SocketTransactionUtils.decodeSocketStream;

class MessageBroker implements Runnable {

    // TODO: How to handle buffer size?
    private static final int BUFFER_SIZE = 4096;

    private final Socket socket;
    private final SocketContext context;
    private final Map<String, Handler> handlers = new HashMap<>();
    private final Gson converter = new Gson();
    private final MessageFormatter msgFormatter = new BasicMessageFormatter();  // TODO: make it a plugin

    public MessageBroker(Socket socket, SocketContext ctx, Map<Class, Map<String, Controller>> controllers) {
        this.socket = socket;
        this.context = ctx;
        initHandlers(controllers);
    }

    private void initHandlers(Map<Class,Map<String,Controller>> controllers) {
        try {
            for (Class handlerClass : controllers.keySet()) {
                Object instance = handlerClass.newInstance();
                Map<String, Controller> currentHandler = controllers.get(handlerClass);
                for (String route : currentHandler.keySet()) {
                    handlers.put(route, new Handler(
                            instance,
                            currentHandler.get(route).getMethod(),
                            currentHandler.get(route).getType(),
                            converter
                    ));
                }
            }
        } catch (IllegalAccessException | InstantiationException e) {
            System.out.println("Could not create controller object. HINT: it needs to have a default constructor!");
            System.exit(0);
        }
    }

    private void processMessage(String msg) {
        // TODO: structural weakness, it should handle a nested object instead of special string
        try {
            msgFormatter.processMessage(msg);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        handlers.get(msgFormatter.getRoute())
                .handle(context, msgFormatter.getRawPayload());
    }

    @Override
    public void run() {
        try (
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                OutputStream out = socket.getOutputStream()
        ) {

            boolean isConnectionValid = SocketTransactionUtils.handleHandshake(inputStream, out);
            if (!isConnectionValid) {
                System.out.println("Invalid handshake attempt was received. Thread broken.");
                return;
            }
            
            System.out.println("Listening for incoming messages...");
            byte[] stream = new byte[BUFFER_SIZE];
            int inputLength;
            String msg;
            boolean validationNeeded = SocketTransactionUtils.authGuardPresent();
            System.out.println(validationNeeded);

            while (context.connected()) {
                inputLength = inputStream.read(stream);
                if (inputLength != -1) {
                    msg = decodeSocketStream(stream, inputLength);
                    if (msg == null || msg.equals("EOF")) {
                        break;
                    }
                    if (validationNeeded) {
                        boolean authenticationIsValid = SocketTransactionUtils.intercept(msg);
                        if (!authenticationIsValid) {
                            break;
                        }
                        validationNeeded = false;
                    } else {
                        System.out.println("Processing " + msg);
                        processMessage(msg);
                    }
                    stream = new byte[BUFFER_SIZE];
                }
            }
            System.out.println("Messagebroker stopped normally.");

        } catch (IOException e) {
            System.out.println("Messagebroker connection lost.");
        } finally {
            context.getUser().sendMessage("EOF");
        }
    }
}
