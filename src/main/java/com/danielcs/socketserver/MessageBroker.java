package com.danielcs.socketserver;

import com.google.gson.Gson;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MessageBroker implements Runnable {

    static final String SEPARATOR = "&&";
    private final Socket socket;
    private final SocketServer.EmitterImpl emitterImpl;
    private Map<String, Handler> handlers = new HashMap<>();
    private Gson converter = new Gson();

    public MessageBroker(Socket socket, SocketServer.EmitterImpl emitterImpl, Map<Class, Map<String, Controller>> controllers) {
        this.socket = socket;
        this.emitterImpl = emitterImpl;
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
                            currentHandler.get(route).getType()
                    ));
                }
            }
        } catch (IllegalAccessException | InstantiationException e) {
            System.out.println("Could not create controller object. HINT: it needs to have a default constructor!");
            System.exit(0);
        }
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
                if (msg.equals("EOF")) break;
                System.out.println(msg); // SERVER LOG
                processMessage(msg);
            }
            System.out.println("Messagebroker stopped normally.");

        } catch (IOException e) {
            System.out.println("Messagebroker connection lost.");
        }
    }

    private void processMessage(String msg) {
        String[] fullMsg = msg.split(SEPARATOR);
        String route = fullMsg[0];
        String payload = fullMsg[1];
        // TODO: NULL CHECK
        handlers.get(route).handle(emitterImpl, payload);
    }

    private final class Handler {

        private final Object obj;
        private final Method method;
        private final Class type;

        Handler(Object obj, Method method, Class type) {
            this.obj = obj;
            this.method = method;
            this.type = type;
        }

        void handle(SocketServer.EmitterImpl emitterImpl, String rawInput) {
            // TODO: wrong input needs to be handled
            Object payload = type == String.class ? rawInput : converter.fromJson(rawInput, type);
            try {
                method.invoke(obj, emitterImpl, payload);
            } catch (IllegalAccessException | InvocationTargetException e) {
                System.out.println("Handler call failed: " + method.getName());
            }
        }
    }
}
