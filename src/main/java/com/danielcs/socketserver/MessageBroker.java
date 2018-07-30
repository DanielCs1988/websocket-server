package com.danielcs.socketserver;

import com.google.gson.Gson;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static com.danielcs.socketserver.SocketTransactionUtils.decodeSocketStream;

class MessageBroker implements Runnable {

    // TODO: Make buffer size dynamic
    private static final int BUFFER_SIZE = 4096;

    private final Socket socket;
    private final BasicContext context;

    private final Map<String, Handler> handlers = new HashMap<>();
    private final Gson converter = new Gson();
    private final MessageFormatter msgFormatter = new BasicMessageFormatter();  // TODO: make it a plugin
    private Caller connectHandler;
    private Caller disconnectHandler;

    MessageBroker(
            Socket socket, BasicContext ctx,
            Map<Class, Map<String, Controller>> controllers,
            SocketServer server
    ) {
        this.socket = socket;
        this.context = ctx;
        processControllers(controllers, server);
    }

    private void processControllers(Map<Class, Map<String, Controller>> controllers, SocketServer server) {
        try {
            for (Class handlerClass : controllers.keySet()) {
                Object instance = server.injectDependencies(handlerClass);
                Map<String, Controller> currentHandler = controllers.get(handlerClass);
                addHandlers(instance, currentHandler);
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            System.out.println("Could not create controller object. HINT: it needs to have a default constructor!");
            System.exit(0);
        }
    }

    private void addHandlers(Object instance, Map<String, Controller> currentHandler) {
        for (String route : currentHandler.keySet()) {
            switch (route) {
                case "connect":
                    connectHandler = new Caller(instance, currentHandler.get(route).getMethod());
                    break;
                case "disconnect":
                    disconnectHandler = new Caller(instance, currentHandler.get(route).getMethod());
                    break;
                default:
                    handlers.put(route, new Handler(
                            instance,
                            currentHandler.get(route).getMethod(),
                            currentHandler.get(route).getType(),
                            converter
                    ));
            }
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
        Handler handler = handlers.get(msgFormatter.getRoute());
        if (handler != null) {
            context.setCurrentRoute(msgFormatter.getRoute());
            handler.handle(context, msgFormatter.getRawPayload());
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
