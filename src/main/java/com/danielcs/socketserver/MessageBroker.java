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

    // TODO: How to handle buffer size?
    private static final int BUFFER_SIZE = 4096;

    private final Socket socket;
    private final BasicContext context;
    private final Map<String, Handler> handlers = new HashMap<>();
    private final Gson converter = new Gson();
    private final MessageFormatter msgFormatter = new BasicMessageFormatter();  // TODO: make it a plugin
    private final Map<Class, Object> dependencies;
    private Caller connectHandler;
    private Caller disconnectHandler;

    public MessageBroker(Socket socket, BasicContext ctx, Map<Class, Map<String, Controller>> controllers, Map<Class, Object> dependencies) {
        this.socket = socket;
        this.context = ctx;
        this.dependencies = dependencies;
        processControllers(controllers);
    }

    private void processControllers(Map<Class, Map<String, Controller>> controllers) {
        try {
            for (Class handlerClass : controllers.keySet()) {
                Object instance = injectDependencies(handlerClass);
                Map<String, Controller> currentHandler = controllers.get(handlerClass);
                addHandlers(instance, currentHandler);
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            System.out.println("Could not create controller object. HINT: it needs to have a default constructor!");
            System.exit(0);
        }
    }

    private Object injectDependencies(Class handlerClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor constructor = handlerClass.getConstructors()[0];
        Class[] paramClasses =  constructor.getParameterTypes();
        Object[] params = new Object[paramClasses.length];
        for (int i = 0; i < paramClasses.length; i++) {
            params[i] = dependencies.getOrDefault(paramClasses[i], null);
        }
        return params.length > 0 ? constructor.newInstance(params) : constructor.newInstance();
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

            boolean isConnectionValid = SocketTransactionUtils.handleHandshake(inputStream, out);
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
