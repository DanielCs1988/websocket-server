package com.danielcs.socketserver;

import com.google.gson.Gson;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class SocketServer {

    private final int PORT;
    private final String CLASSPATH;

    private final ExecutorService connectionPool;
    private final Map<Integer, ArrayBlockingQueue<String>> messages = Collections.synchronizedMap(new HashMap<>());
    private final Map<Class, Map<String, Controller>> controllers = new HashMap<>();

    public SocketServer(int port, String classPath, int poolSize) {
        this.PORT = port;
        this.CLASSPATH = classPath;
        connectionPool = Executors.newFixedThreadPool(poolSize * 2);
        setupControllers();
    }

    private Set<Class<?>> scanClassPath() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(CLASSPATH))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner())
        );
        return reflections.getTypesAnnotatedWith(SocketController.class);
    }

    private void setupControllers() {
        Set<Class<?>> handlerClasses = scanClassPath();
        for (Class handlerClass : handlerClasses) {
            controllers.put(handlerClass, new HashMap<>());
            for (Method method : handlerClass.getMethods()) {
                if (method.isAnnotationPresent(SocketHandler.class)) {
                    SocketHandler config = method.getAnnotation(SocketHandler.class);
                    controllers.get(handlerClass).put(config.route(), new Controller(method, config.type()));
                }
            }
        }
    }

    public void start() {

        try (ServerSocket server = new ServerSocket(PORT)) {

            int currentUserId = 1;
            while (true) {
                Socket client = server.accept();
                messages.put(currentUserId, new ArrayBlockingQueue<>(2));
                EmitterImpl emitterImpl = new EmitterImpl(currentUserId);

                MessageSender handler = new MessageSender(client, messages.get(currentUserId));
                MessageBroker broker = new MessageBroker(client, emitterImpl, controllers);
                connectionPool.execute(handler);
                connectionPool.execute(broker);

                System.out.println("Client connected: " + currentUserId);
                currentUserId ++;
            }

        } catch (IOException e) {
            System.out.println("Could not open server-side socket connection.");
        }
        connectionPool.shutdownNow();
        System.out.println("Server closed down.");
    }

    public static void main(String[] args) {
        SocketServer server = new SocketServer(5000, "com.danielcs.socketserver.controllers", 10);
        server.start();
    }

    class EmitterImpl implements Emitter {

        private final ArrayBlockingQueue<String> clientMessageQueue;
        private final Gson converter = new Gson();

        EmitterImpl(int clientId) {
            clientMessageQueue = messages.get(clientId);
        }

        public void reply(String path, Object payload) {
            // TODO: extract message encoding, maybe also: handle string payload
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            clientMessageQueue.offer(msg);
        }

        public void emit(String path, Object payload) {
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            for (ArrayBlockingQueue<String> msgQueue : messages.values()) {
                msgQueue.offer(msg);
            }
        }
    }
}

class Controller {

    private Method method;
    private Class type;

    public Controller(Method method, Class type) {
        this.method = method;
        this.type = type;
    }

    public Method getMethod() {
        return method;
    }

    public Class getType() {
        return type;
    }
}
