package com.danielcs.socketserver;

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
    // MAY work better with a MAP, too many lookups
    private final Set<UserSession> users = Collections.synchronizedSet(new HashSet<>());
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

            while (true) {
                Socket client = server.accept();

                UserSession user = new UserSession();
                users.add(user);
                SocketContext ctx = new BasicContext(user, users);
                MessageSender handler = new MessageSender(client, user);
                MessageBroker broker = new MessageBroker(client, ctx, controllers);

                connectionPool.execute(handler);
                connectionPool.execute(broker);
                System.out.println("Client connected: " + user);
            }

        } catch (IOException e) {
            System.out.println("Could not open server-side socket connection.");
        }
        connectionPool.shutdownNow();
        System.out.println("Server closed down.");
    }
}
