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
                SocketContext ctx = new SocketContextImpl(user);

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

    public static void main(String[] args) {
        SocketServer server = new SocketServer(5500, "com.danielcs.socketserver.controllers", 10);
        server.start();
    }

    class SocketContextImpl implements SocketContext {
        // TODO: I should pull this out to a separate class
        private boolean connected = true;
        private final Gson converter = new Gson();
        private final UserSession user;

        SocketContextImpl(UserSession user) {
            this.user = user;
        }

        @Override
        public boolean connected() {
            return connected;
        }

        @Override
        public UserSession getUser() {
            return user;
        }

        @Override
        public void reply(String path, Object payload) {
            // TODO: extract message encoding, maybe also: handle string payload
            // TODO: separator was outsourced to the MessageFormat object
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            user.sendMessage(msg);
        }

        @Override
        public void emit(String path, Object payload) {
            // TODO: when separated, this method could access users by providing a method in SocketServer
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            users.forEach(user -> user.sendMessage(msg));
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

        @Override
        public void emitToRoom(String room, String path, Object payload) {
            // TODO: get rid of internal double conversion
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            Room.getUsersInRoom(room).forEach(user -> user.sendMessage(msg));
        }

        @Override
        public void sendToUser(int userId, String path, Object payload) {
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            users.stream()
                    .filter(usr -> usr.getId() == userId)
                    .findFirst()
                    .ifPresent(usr -> usr.sendMessage(msg));
        }

        @Override
        public void sendToUser(String propertyName, String propertyValue, String path, Object payload) {
            String msg = path + MessageBroker.SEPARATOR + converter.toJson(payload);
            users.stream()
                    .filter(usr -> usr.getProperty(propertyName).equals(propertyValue))
                    .forEach(usr -> usr.sendMessage(msg));
        }

        @Override
        public void disconnect() {
            connected = false;
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
