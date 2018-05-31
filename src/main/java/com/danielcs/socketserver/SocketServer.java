package com.danielcs.socketserver;

import com.google.gson.Gson;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class SocketServer {

    private final int PORT;
    private final String CLASSPATH;

    private final ExecutorService connectionPool;
    private final Map<UserSession, ArrayBlockingQueue<String>> messages = Collections.synchronizedMap(new HashMap<>());
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
        /*for (Class<? extends UserSession> aClass : reflections.getSubTypesOf(UserSession.class)) {
            try {
                System.out.println(aClass.getConstructor(String.class).newInstance("Hey I'm in da subclass!").toString());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }*/
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
                UserSession user = new DummyUser("RandomJoe");
                messages.put(user, new ArrayBlockingQueue<>(2));
                SocketContext ctx = new SocketContextImpl(user);

                MessageSender handler = new MessageSender(client, messages.get(user));
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

    public static void main(String[] args) {
        SocketServer server = new SocketServer(5000, "com.danielcs.socketserver.controllers", 10);
        server.start();
    }

    class SocketContextImpl implements SocketContext {

        private final ArrayBlockingQueue<String> clientMessageQueue;
        private final Gson converter = new Gson();
        private final UserSession user;

        SocketContextImpl(UserSession user) {
            this.user = user;
            clientMessageQueue = messages.get(user);
        }

        @Override
        public void reply(String path, Object payload) {
            // TODO: extract message encoding, maybe also: handle string payload
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            clientMessageQueue.offer(msg);
        }

        @Override
        public void emit(String path, Object payload) {
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            for (ArrayBlockingQueue<String> msgQueue : messages.values()) {
                msgQueue.offer(msg);
            }
        }

        @Override
        public void joinRoom(String name) {
            Room.joinRoom(user, name);
        }

        @Override
        public void leaveRoom(String name) {
            Room.leaveRoom(user, name);
        }

        @Override
        public void leaveAllRooms() {
            Room.leaveCurrentRooms(user);
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

// TODO: switch this to classpath scanning with TYPE annotation
class DummyUser extends UserSession {

    private String name;

    public DummyUser(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DummyUser{" +
                "name='" + name + '\'' +
                "id=" + getId() +
                "} ";
    }
}
